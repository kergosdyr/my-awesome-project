# Commerce Labs: Load-Test Results

2026-08-11 KST에 Apple M4 로컬 단일 호스트에서 실행한 세 가지 커머스 실험의 결과다. 각 비교군은 같은 lab commit, fixture, 컨테이너 제한, 도착률 프로파일을 사용했고 독립 실행 3회의 run-level 중앙값을 대표값으로 삼았다. 원시 `summary.json`, backend counter, 환경 캡처와 실패 응답은 [`raw/`](raw/)에 그대로 보존했다.

## 결론 먼저

| Lab | 보호하려는 것 | 관찰한 효과 | 지불한 비용 |
| --- | --- | --- | --- |
| Cache stampede | TTL 만료 시 MySQL 중복 조회 | origin load `985 → 119`, **87.9% 감소**; p95 `42.319 → 24.265ms`, 42.7% 감소 | p99는 1.7%만 감소했고 최대 지연 중앙값은 13.0% 증가 |
| Kafka transactional outbox | DB commit과 event 발행 의도의 원자성 | broker 중단 중 direct는 503/DB rollback 뒤 orphan event가 남았고, outbox는 201/DB pending 뒤 복구 | 정상 경로 p99 `28.480 → 33.239ms`, 최대 지연 중앙값 61.4% 증가; relay·중복·backlog 운영 필요 |
| Redis waiting room | 동시에 실행되는 구매 작업 상한 | 수락률 `72.736% → 100%` (**+27.264%p**), max active 4, FIFO 위반 0 | 수락 E2E p50 `52ms → 14.055s`; HTTP 요청 중앙값 약 **24.0배** |

세 실험은 같은 종류의 최적화가 아니다. Cache lock은 같은 결과를 기다릴 수 있는 읽기 증폭을 줄인다. Outbox는 API latency보다 장애 시 유실·모호성의 경계를 바꾼다. Waiting room은 4개의 downstream slot을 늘리지 않고 즉시 거절을 순서 있는 대기로 바꾼다.

## 1. Cache stampede

질문은 500ms TTL의 hot product가 만료될 때 Redis token lock과 double-check가 오류 없이 slow-origin 중복 실행을 억제하는지였다. Naive와 protected 모두 `20 → 250 RPS`, `10초 상승 + 45초 유지 + 10초 하강`, 40ms 제어 origin 지연에서 약 13,850건씩 처리했다. 6개 실행 모두 오류와 dropped iteration은 0이었다.

| 지표 | Naive 중앙값 | Protected 중앙값 | 변화 |
| --- | ---: | ---: | ---: |
| 성공 RPS | 212.923 | 213.057 | +0.1% |
| p50 | 1.213ms | 1.240ms | +2.2% |
| p95 | 42.319ms | 24.265ms | **-42.7%** |
| p99 | 46.133ms | 45.370ms | -1.7% |
| 최대 지연 | 55.865ms | 63.102ms | **+13.0%** |
| 최초 cache miss | 985 | 1,048 | +6.4% |
| origin load | 985 | 119 | **-87.9%** |
| lock contention | 0 | 929 | 보호 경로 동작 확인 |

가설은 채택한다. 같은 만료 조건에서 origin amplification은 크게 줄었고 도착률을 놓치지 않았다. 다만 waiter도 refill을 기다리므로 40ms origin 지연이 p99에 남았고, protected 한 실행의 max는 92.233ms였다. 이것은 DB 보호 효과이지 모든 tail latency의 제거가 아니다.

Redis 중단은 `503 CACHE_UNAVAILABLE`, 강제 lock timeout은 `503 CACHE_LOCK_TIMEOUT`을 반환했고 두 경우 모두 origin fallback은 0이었다. 장애를 DB stampede로 전환하지 않는 fail-closed 정책을 확인했다. 상세 설계와 실행별 표는 [cache lab PR #4](https://github.com/kergosdyr/my-awesome-project/pull/4), 원시는 [`raw/cache-stampede/`](raw/cache-stampede/)에 있다.

## 2. Kafka direct publish와 transactional outbox

두 전략은 같은 주문 command와 `1 → 2 RPS`, `15초 상승 + 30초 유지 + 15초 하강`을 사용했다. 실행마다 97개 주문이 생성됐고 6개 실행 모두 오류·drop 없이 `created = published = processed = 97`로 drain됐다.

| 지표 | Direct 중앙값 | Outbox 중앙값 | 변화 |
| --- | ---: | ---: | ---: |
| 성공 RPS | 1.693389 | 1.693336 | -0.003% |
| p50 | 21.782ms | 17.314ms | -20.5% |
| p95 | 26.726ms | 23.996ms | -10.2% |
| p99 | 28.480ms | 33.239ms | **+16.7%** |
| 최대 지연 | 32.481ms | 52.416ms | **+61.4%** |
| 종료 시 created/published/processed | 97/97/97 | 97/97/97 | 동일 |

Outbox의 p50/p95가 낮았지만 일반적인 latency 우위로 해석하지 않는다. Direct 세 번 뒤 outbox 세 번이라는 고정 순서였고, 두 전략 모두 포화와 거리가 먼 저부하였다. 이 실험에서 더 중요한 근거는 broker 중단 결과다.

| Broker 중단 관찰 | Direct | Outbox |
| --- | --- | --- |
| HTTP | `503 EVENT_PUBLISH_FAILED`, 5.021s | `201 Created`, 9.328ms |
| 응답 직후 DB | order 0, outbox 0, processed 0 | order 1, outbox 1 pending, processed 0 |
| 복구 후 | DB order가 없는 event 1개 소비 | pending 0, 정상 event 1개 처리 |
| 중복 | 직접 실패까지 포함한 비결정적 publish 결과 | relay 재전달 2회, unique `event_id`로 side effect 1회 |

Direct 요청은 DB rollback 뒤에도 producer future가 재기동된 broker에 event를 전달했다. DB에 없는 order 584의 event가 남았으므로 dual-write 모호성이 실제로 드러났다. 실험 순서와 DB 상태상 direct 실패의 event라는 근거가 강하지만 실패 응답에 correlation ID가 없어 이 귀속은 **추론**이다.

Outbox는 중단 중에도 주문과 발행 의도를 한 DB transaction으로 남겼다. 해당 row는 발생 후 14.576초에 published가 됐다. 이 값은 consumer end-to-end latency가 아니라 **outbox occurrence-to-publish 복구 시간**이다. 최종 `publishFailures=3`은 direct 1회와 relay 2회를 합친 값이며, at-least-once 중복은 consumer 멱등성으로 흡수했다. 상세 내용은 [Kafka lab PR #5](https://github.com/kergosdyr/my-awesome-project/pull/5), 원시는 [`raw/kafka-outbox/`](raw/kafka-outbox/)에 있다.

## 3. Redis waiting room

Direct와 queued는 같은 commit `0e9dcb1`, `10 → 100 iterations/s`, `15초 상승 + 60초 유지 + 15초 하강`에서 교차 순서로 각 3회 실행했다. Downstream은 최대 4개 작업을 50ms씩 처리한다. Queue는 Redis ZSET sequence의 현재 head만 자신의 poll에서 입장시키고, 일회용 token을 같은 응답으로 돌려준다.

| 지표 | Direct 중앙값 | Queued 중앙값 | 변화/비용 |
| --- | ---: | ---: | ---: |
| 총 iteration | 7,575 | 7,574 | 사실상 동일한 exposure |
| accepted | 5,509 | 7,574 | +2,065 |
| rejected | 2,065 | 0 | -2,065 |
| 수락률 | 72.736% | 100% | **+27.264%p** |
| accepted E2E p50 | 52ms | 14,054.5ms | +14.003s |
| accepted E2E p95 | 60ms | 27,880.8ms | +27.821s |
| accepted E2E p99 | 61ms | 28,255ms | +28.194s |
| queue wait p50/p95/p99 | 해당 없음 | 14,003 / 27,828.8 / 28,202ms | 대기 비용 |
| 전체 HTTP 요청 | 7,954 | 191,071 | **약 24.0배** |
| max active | 4 | 4 | 상한 준수 |
| max queue depth | 0 | 1,960 | backlog 가시화 |
| FIFO violations | 해당 없음 | 0 | 3회 모두 0 |

각 행은 지표별 run-level 중앙값이므로 direct의 대표 count `5,509 + 2,065`가 별도로 계산한 iteration 중앙값 `7,575`와 1 차이 난다. 실행별로는 항상 `accepted + rejected = iterations`가 정확히 성립했다.

Queued는 3회 모두 `issued = admitted = completed = downstream accepted = 7,574`로 정확히 일치했고 expired, downstream rejection, scenario error, dropped iteration은 모두 0이었다. 즉, 이 조건에서는 overload를 429 실패가 아닌 순서 있는 대기로 전환했다. 용량은 여전히 4이므로 tail 사용자는 약 28초를 기다렸고 polling 때문에 전체 HTTP 요청은 약 24배가 됐다.

Direct의 k6 built-in HTTP failure 중앙값 약 26%는 의도한 429를 포함한다. 서버 오류와 response-contract 오류는 0이지만 사업 결과의 27.264%는 거절이므로 “오류 0”으로 요약하지 않는다. Queued의 개별 HTTP p95/p99 threshold도 통과했지만 사용자 E2E p95 27.881초는 현재 threshold 대상이 아니다. 최악 wait 28.272초는 30초 ticket TTL에 1.728초까지 접근했으므로 조금 더 긴 burst에서도 expiry 0이라고 일반화할 수 없다.

두 모드는 같은 open-model arrival을 썼지만 load generator 설정은 direct `200/1,000 VU`, queued `3,000/4,000 VU`였다. Queued의 실제 max VU 중앙값은 1,962였고 arrival 종료 뒤 backlog drain으로 wall time이 direct보다 약 23.1초 길었다. 이것 역시 서버 용량 증가가 아니라 client-side waiting의 비용이다.

위 수치는 최종 6개 실행만 사용한다. 개발 중 `14bc9ed`와 `c6d1237` preflight는 VU/HTTP 증폭으로 중단됐거나 불완전했으며 계산에서 제외했다. 가장 큰 중단 실행의 `473,624`는 endpoint별 poll 수가 아닌 전체 HTTP 요청이다. 원본은 실패 과정도 학습할 수 있도록 삭제하지 않고 별도 표시했다.

추가 정책 검증은 forged token 403, valid token 200, replay 410, Redis 중단 시 queue API 503/direct 비교군 200, admission lease 만료 뒤 slot 재개방을 확인한다. 상세 설계와 결과는 [waiting-room lab PR #6](https://github.com/kergosdyr/my-awesome-project/pull/6), 원시는 [`raw/waiting-room/`](raw/waiting-room/)에 있다.

## 범위와 지표 정의

- **대표값:** 요청을 합쳐 재계산하지 않고 독립 실행 3개의 run-level 값 중앙값을 사용한다.
- **p50/p95/p99:** 명시한 scenario의 k6 trend percentile이다. Waiting-room E2E는 ticket 발급부터 purchase 응답까지다.
- **Origin load:** cache miss 추정치가 아니라 backend가 실제 slow-origin 진입을 센 값이다.
- **Accepted/rejected:** HTTP status와 response contract를 함께 통과한 scenario-specific counter다.
- **FIFO violation:** 더 작은 eligible sequence가 기다리는 동안 더 큰 sequence가 admission된 경우의 backend counter다.
- **Scenario 오류:** 실험이 의도한 429/202는 custom scenario error가 아니다. 다만 k6 built-in `http_req_failed`는 429를 failure로 세므로 보고서에서 두 의미를 구분한다.

환경은 macOS 26.5.1, Apple M4 10-core, 16GiB, Docker Engine 28.5.2/OrbStack, k6 0.56.0, Java 21이다. 각 실행은 reset, 환경 캡처, k6 summary, backend metrics, Prometheus 종료 snapshot 순서로 기록했다. Cache와 Kafka는 비교 순서가 고정됐고 waiting room은 direct/queued 순서를 run마다 교차했다.

## 한계와 다음 실험

이 결과는 로컬 단일 backend, 단일 Redis, 단일 MySQL, 단일 Kafka broker의 짧은 제어 실험이다. 운영 용량이나 SLA를 보장하지 않는다. CPU, heap, GC, DB lock wait, Redis command latency, Kafka producer/consumer lag의 시계열도 없어 원인별 자원 비용을 완전히 분해할 수 없다.

다음 순서가 가장 유용하다.

1. 같은 runner를 CI 전용 고정 사양 host에서 반복하고 commit 간 회귀 임계값을 만든다.
2. Prometheus/Grafana로 CPU·GC·pool·Redis·Kafka lag 시계열을 함께 저장한다.
3. Cache는 다중 backend, TTL jitter, stale-while-revalidate, lock lease보다 긴 origin을 비교한다.
4. Kafka는 다중 relay와 `SKIP LOCKED`, retry/backoff, poison event, ack 직후 crash를 검증한다.
5. Waiting room은 SSE/WebSocket 또는 long polling, ticket expiry/재접속, 다중 backend·Redis failover를 비교해 polling 24배 비용을 줄인다.
6. Stock concurrency lab에서 비관/낙관 lock과 queue를 실제 재고 차감 command에 연결한다.

기계 검증 가능한 chart/table snapshot은 [`load-test-report.artifact.json`](load-test-report.artifact.json), 브라우저에서 바로 보는 portable 버전은 [`load-test-report.html`](load-test-report.html)에 제공한다. 보고용 계산 규칙과 출처 목록은 [`source-notes.md`](source-notes.md)에 있다.
