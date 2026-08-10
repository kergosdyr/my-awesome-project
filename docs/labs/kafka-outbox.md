# Lab: Kafka direct publish vs transactional outbox

- 상태: 로컬 측정 완료 (2026-08-11 KST)
- 관련 이슈: [#2](https://github.com/kergosdyr/my-awesome-project/issues/2)

## 질문과 가설

질문: 주문 트랜잭션 안에서 Kafka broker ack를 기다리는 방식과 주문·outbox를 원자적으로 저장한 뒤 비동기로 발행하는 방식은 응답 지연, 결합도, 장애 시 유실·중복 특성이 어떻게 다른가?

가설: outbox 전략은 요청 경로에서 broker 왕복을 제거해 Kafka 지연·중단 시에도 주문 생성 응답을 유지한다. 대신 end-to-end 처리 지연과 운영해야 할 backlog가 생기며, relay의 at-least-once 발행 때문에 consumer 멱등성이 필수다.

## 동일하게 유지하는 계약

- 두 전략 모두 `POST /api/labs/events/orders?strategy=...`와 같은 주문 payload를 사용한다.
- 주문 생성은 기존 `OrderPlacer`를 공유하며 상품을 ID 순으로 `SELECT ... FOR UPDATE`한 뒤 주문·주문선을 저장한다.
- 표준 `POST /api/orders` 계약과 동작은 변경하지 않는다.
- Kafka key는 `orderNumber`, topic은 기본 `commerce-lab.order-placed.v1`, producer는 `acks=all`과 idempotence를 사용한다.
- consumer side effect는 `processed_order_events.event_id` 기본 키로 한 번만 기록한다. Kafka producer idempotence는 producer session의 재시도를 줄일 뿐 end-to-end exactly-once를 제공하지 않는다.

## 트랜잭션 경계와 의미

| 전략/entry point | DB 트랜잭션 | Kafka 상호작용 | 보장과 의도된 위험 |
| --- | --- | --- | --- |
| `strategy=direct` HTTP | 상품 lock + 주문 저장 + broker ack 대기를 한 요청 트랜잭션에서 수행 | 요청 thread가 ack까지 대기 | publish 실패는 DB rollback. ack 뒤 DB commit 실패 시 Kafka에는 존재하지만 주문은 없는 dual-write window가 남고, broker 지연 동안 DB lock/connection을 점유한다. |
| `strategy=outbox` HTTP | 상품 lock + 주문 + outbox insert를 하나의 DB 트랜잭션으로 commit | 없음 | 주문과 발행 의도가 원자적이다. 응답 시점에는 아직 소비되지 않았을 수 있다. |
| outbox scheduler | 가장 오래된 pending batch를 lock한 별도 트랜잭션 | 각 row의 broker ack를 기다린 뒤 published 처리 | ack 뒤 DB commit 전 crash 시 재발행될 수 있어 at-least-once다. 실패 row는 pending 유지, attempt/failure/error를 기록한다. |
| Kafka consumer | listener 자체에는 트랜잭션이 없고 processing facade가 delivery 하나의 DB 트랜잭션을 소유 | facade 성공 뒤 record ack | 동일 event id의 재전달은 unique key insert가 무시되어 side effect가 중복되지 않는다. |

Outbox relay는 단일 lab instance에서 행 잠금과 작은 batch를 사용한다. broker ack 동안 outbox row lock을 유지하는 단순한 교육용 구현이며, 운영 환경의 다중 relay에서는 claim/lease, retry backoff, poison-event 격리와 `SKIP LOCKED` 전략을 별도 실험해야 한다.

## API와 지표

```text
POST /api/labs/events/orders?strategy=direct
POST /api/labs/events/orders?strategy=outbox
GET  /api/labs/events/metrics
POST /api/labs/events/reset
```

metrics는 `created`, `published`, `processed`, `duplicates`, `pending`, `oldestPendingAgeMs`, `publishFailures`를 반환한다. created/published/duplicates와 direct publish failure는 현재 프로세스의 resettable counter이고, processed·pending·oldest age와 outbox publish failure는 DB에서 읽는다. 프로세스를 재시작한 실행은 이전 실행과 섞지 않는다.

reset은 기본적으로 비활성화되어 있고 compose의 로컬 backend에서만 `APP_LABS_KAFKA_OUTBOX_RESET_ENABLED=true`로 켠다. 처리/outbox/주문 fixture를 지우고 V2 seed의 상품 1~6 재고와 상태를 복원하므로 공유 환경에서는 절대 활성화하지 않는다.

## 재현 명령

저장소 루트에서 실행한다.

```bash
docker compose config
docker compose up --build --detach
curl --fail http://localhost:8080/actuator/health
curl --fail http://localhost:8080/api/labs/events/metrics
```

짧은 기능 확인:

```bash
curl --fail --request POST http://localhost:8080/api/labs/events/reset

curl --fail --request POST \
  'http://localhost:8080/api/labs/events/orders?strategy=outbox' \
  --header 'Content-Type: application/json' \
  --data '{"customerName":"lab-user","lines":[{"productId":1,"quantity":1}]}'

curl --fail http://localhost:8080/api/labs/events/metrics
```

두 전략의 동일 부하 비교:

```bash
export BASE_URL=http://localhost:8080
export RESULTS_DIR="$PWD/load-tests/results"
export LAB_RUN_PREFIX="kafka-outbox-$(git rev-parse --short HEAD)"

export EVENT_LAB_START_RATE=1
export EVENT_LAB_PEAK_RATE=2
export EVENT_LAB_RAMP_UP=15s
export EVENT_LAB_STEADY=30s
export EVENT_LAB_RAMP_DOWN=15s
export EVENT_LAB_PRE_ALLOCATED_VUS=20
export EVENT_LAB_MAX_VUS=100
export EVENT_LAB_PRODUCT_ID=1
export EVENT_LAB_QUANTITY=1

bash load-tests/run-kafka-outbox-lab.sh
```

runner는 각 전략 직전에 reset하고, k6 원본 요약과 환경 정보에 더해 drain 이후 `backend-metrics.json`을 저장한다. 기본 traffic은 seed 재고 120개 안에서 끝나도록 작게 잡았다. RPS/시간을 늘리려면 먼저 fixture 재고도 늘리고 두 전략에 같은 값을 적용한다.

## 실패 및 멱등성 점검

Direct broker coupling:

```bash
docker compose stop kafka
curl --request POST \
  'http://localhost:8080/api/labs/events/orders?strategy=direct' \
  --header 'Content-Type: application/json' \
  --data '{"customerName":"direct-failure","lines":[{"productId":1,"quantity":1}]}'
curl --fail http://localhost:8080/api/labs/events/metrics
docker compose start kafka
```

HTTP 503과 주문/재고 rollback을 확인한다. `publishFailures`가 증가해야 한다.

Outbox recovery:

```bash
docker compose stop kafka
curl --fail --request POST \
  'http://localhost:8080/api/labs/events/orders?strategy=outbox' \
  --header 'Content-Type: application/json' \
  --data '{"customerName":"outbox-failure","lines":[{"productId":1,"quantity":1}]}'
curl --fail http://localhost:8080/api/labs/events/metrics
docker compose start kafka
```

Kafka가 멈춰도 HTTP 201과 `pending >= 1`을 확인한다. 재시작 후 pending이 0이 되고 processed가 증가하는 복구 시간을 기록한다.

Consumer duplicate check는 이미 처리된 outbox payload를 같은 topic에 두 번 다시 넣는다.

```bash
event_payload="$(docker compose exec -T mysql sh -lc \
  'MYSQL_PWD="$MYSQL_PASSWORD" mysql --batch --skip-column-names \
  --user="$MYSQL_USER" "$MYSQL_DATABASE" \
  --execute="select payload from order_events_outbox order by occurred_at desc limit 1"')"

printf '%s\n%s\n' "$event_payload" "$event_payload" | \
  docker compose exec -T kafka \
  /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:19092 \
  --topic commerce-lab.order-placed.v1

curl --fail http://localhost:8080/api/labs/events/metrics
```

동일 event id는 `processed_order_events`에 한 행만 남고 `duplicates`만 2 증가해야 한다. 자동화된 API test도 이 DB 불변식을 검증한다. relay의 ack 직후 강제 종료로 실제 재전달 window를 점검할 때는 종료·재시작 시각을 결과 문서에 남긴다.

## 측정 결과

정상 broker 상태에서 두 전략을 동일한 `1 → 2 RPS`, `15초 상승 + 30초 유지 + 15초 하강` 프로파일로 각각 3회 실행했다. 실행마다 fixture와 counter를 reset했고, 97개 주문의 run-level 중앙값을 대표값으로 사용했다. 모든 실행에서 오류와 dropped iteration은 0이었으며 종료 후 `created = published = processed = 97`, `pending = duplicates = publishFailures = 0`이었다.

| 지표 | direct 중앙값 | outbox 중앙값 | outbox 변화/해석 |
| --- | ---: | ---: | --- |
| 성공 RPS | 1.693389 | 1.693336 | -0.003%; 도착률에 의해 고정 |
| 응답 시간 p50 | 21.782ms | 17.314ms | **-20.5%** |
| 응답 시간 p95 | 26.726ms | 23.996ms | **-10.2%** |
| 응답 시간 p99 | 28.480ms | 33.239ms | **+16.7%** |
| run 최대 지연 중앙값 | 32.481ms | 52.416ms | **+61.4%** |
| HTTP 오류율 / dropped | 0% / 0 | 0% / 0 | 동일 |
| 종료 시 created / published / processed | 97 / 97 / 97 | 97 / 97 / 97 | 모두 drain 완료 |
| 종료 시 duplicates / pending / failures | 0 / 0 / 0 | 0 / 0 / 0 | 정상 경로 동일 |

Outbox의 p50과 p95는 낮았지만 p99와 최대값은 더 높았다. 세 run 모두 시간이 지날수록 양쪽 지연이 낮아졌고 direct 다음 outbox 순서가 고정되어 있어, 이 짧은 저부하 결과만으로 일반적인 latency 우위를 결론 내리지 않는다. 정상 상태에서 관찰한 핵심 차이는 처리량이 아니라 broker ack가 요청 transaction 안에 있는지 여부다.

### 실행별 결과

| 전략 | 실행 | 요청 | 성공 RPS | p50(ms) | p95(ms) | p99(ms) | max(ms) | 종료 created/published/processed |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| direct | 1 | 97 | 1.693161 | 25.450 | 30.632 | 34.991 | 45.872 | 97 / 97 / 97 |
| direct | 2 | 97 | 1.693389 | 21.782 | 26.726 | 28.480 | 32.481 | 97 / 97 / 97 |
| direct | 3 | 97 | 1.693546 | 19.358 | 22.435 | 23.940 | 24.183 | 97 / 97 / 97 |
| outbox | 1 | 97 | 1.693320 | 19.581 | 24.711 | 35.592 | 52.416 | 97 / 97 / 97 |
| outbox | 2 | 97 | 1.693336 | 17.314 | 23.996 | 29.181 | 79.396 | 97 / 97 / 97 |
| outbox | 3 | 97 | 1.693425 | 15.476 | 18.394 | 33.239 | 37.255 | 97 / 97 / 97 |

### Broker 중단과 복구

정상 실행 뒤 fixture를 다시 reset하고 Kafka를 중지한 상태에서 direct와 outbox 요청을 순서대로 한 번씩 보냈다.

| 관찰 | direct | outbox |
| --- | --- | --- |
| HTTP 결과 | `503 EVENT_PUBLISH_FAILED` | `201 Created` |
| 요청 시간 | 5.021초 | 9.328ms |
| 응답 직후 DB | orders 0, outbox 0, processed 0 | orders 1, outbox 1, processed 0 |
| 응답 직후 metrics | created 0, published 0, failure 1 | created 1, published 0, pending 1 |
| broker 복구 뒤 | DB에 없는 order id 584의 event가 뒤늦게 소비됨 | pending 0, outbox event 1개가 한 번만 처리됨 |

Direct 요청은 timeout과 DB rollback 뒤에도 producer future가 broker 재기동 후 event를 전달했다. 그 결과 `processed_order_events`에는 존재하지만 `orders`에는 없는 `ORPHAN_EVENT`가 실제로 남았다. 이것은 “HTTP 실패/DB rollback이면 Kafka에도 없다”라고 가정할 수 없는 dual-write의 모호한 결과다.

Outbox 요청은 Kafka 중단 중에도 주문과 event 의도를 같은 DB transaction으로 남겼다. event는 생성 후 14.576초에 published 상태가 되었고, relay는 총 3회 시도 중 2회 실패를 기록했다. Broker가 실제로 받았는지 알 수 없는 재시도 때문에 동일 outbox event가 2회 중복 전달됐지만, `event_id` unique 처리로 side-effect row는 한 개만 남았다. 최종 집계 `processed=2`는 정상 outbox event 한 개와 direct orphan event 한 개이고, `duplicates=2`는 outbox 재전달 두 건이다.

- [direct 실패 응답과 시간](../reports/raw/kafka-outbox/failure-recovery-cb400d9/direct-request-metadata.json), [즉시 DB count](../reports/raw/kafka-outbox/failure-recovery-cb400d9/after-direct-db-counts.txt)
- [outbox 성공 응답과 시간](../reports/raw/kafka-outbox/failure-recovery-cb400d9/outbox-request-metadata.json), [pending metrics](../reports/raw/kafka-outbox/failure-recovery-cb400d9/outbox-pending-metrics.json)
- [복구 후 metrics](../reports/raw/kafka-outbox/failure-recovery-cb400d9/recovered-metrics.json), [event별 DB 분류](../reports/raw/kafka-outbox/failure-recovery-cb400d9/recovered-db-details.tsv)

## 원본 결과와 한계

각 run 디렉터리에는 원본 k6 `summary.json`/`summary.txt`, 환경 캡처, drain 뒤 backend metrics가 있다.

- [direct run 1](../reports/raw/kafka-outbox/kafka-outbox-run1-cb400d9-direct/summary.json), [run 2](../reports/raw/kafka-outbox/kafka-outbox-run2-cb400d9-direct/summary.json), [run 3](../reports/raw/kafka-outbox/kafka-outbox-run3-cb400d9-direct/summary.json)
- [outbox run 1](../reports/raw/kafka-outbox/kafka-outbox-run1-cb400d9-outbox/summary.json), [run 2](../reports/raw/kafka-outbox/kafka-outbox-run2-cb400d9-outbox/summary.json), [run 3](../reports/raw/kafka-outbox/kafka-outbox-run3-cb400d9-outbox/summary.json)

한계:

- 단일 broker·단일 backend·로컬 Docker 조건이며 relay가 DB lock을 잡고 순차 publish한다. 이 결과를 다중 broker, 다중 relay 또는 실제 네트워크 장애 조건으로 일반화하지 않는다.
- 97개 주문, peak 2 RPS는 saturation 시험이 아니라 요청 결합도와 실패 의미를 보는 짧은 실험이다. DB connection/lock wait와 Kafka producer/consumer 시계열은 수집하지 않았다.
- 실행 순서는 direct 다음 outbox로 고정했고 별도 warm-up run을 제외하지 않았다. 중앙값은 cold-start 영향을 줄이지만 순서 효과를 제거하지 않는다.
- 정상 실행은 drain 뒤 최종 counter만 저장해 run 중 backlog 최대값과 event end-to-end 지연 분포를 알 수 없다.
- outbox와 processed table의 DB 출력 timestamp가 서로 다른 timezone 기준으로 렌더링되어 cross-table consumer lag 계산은 제외했다. 같은 outbox row의 `occurred_at → published_at` 14.576초만 사용했다.
- relay ack 직후 process crash를 강제한 시험은 하지 않았다. Broker 중단/복구 자체가 재전달과 멱등성 동작을 재현했다.
