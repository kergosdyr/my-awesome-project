# Lab: Kafka direct publish vs transactional outbox

- 상태: 측정 준비 완료
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

<!-- 실측 전에는 채우지 않는다. 각 전략을 최소 3회 실행하고 중앙값을 기록한다. -->

| 지표 | direct | outbox | 변화/해석 |
| --- | ---: | ---: | --- |
| 성공 RPS |  |  |  |
| 응답 시간 p50 |  |  |  |
| 응답 시간 p95 |  |  |  |
| 응답 시간 p99 |  |  |  |
| HTTP 오류율 |  |  |  |
| created / published / processed |  |  |  |
| duplicates |  |  |  |
| pending 최대값 / oldest age |  |  |  |
| publish failures |  |  |  |
| DB active connections / lock wait |  |  |  |
| Kafka produce latency / consumer lag |  |  |  |

## 원본 결과와 한계

- direct `summary.json`:
- direct `summary.txt`:
- direct `backend-metrics.json`:
- outbox `summary.json`:
- outbox `summary.txt`:
- outbox `backend-metrics.json`:
- CPU/memory/JVM/Kafka 환경 캡처:

한계: 단일 broker·단일 backend·로컬 Docker 조건이며 relay가 DB lock을 잡고 순차 publish한다. 이 결과를 다중 broker, 다중 relay 또는 실제 네트워크 장애 조건으로 일반화하지 않는다.
