# Lab: Redis FIFO 대기열

- 상태: 진행 중
- 작성일: 2026-08-11
- 관련 Issue: [#3](https://github.com/kergosdyr/my-awesome-project/issues/3)

## 질문

동일한 50ms 구매 처리와 동시 처리 상한에서, 직접 요청과 Redis FIFO 대기열은 각각 과부하 거절, 활성 구매자 수, 대기 시간, 순서 공정성을 어떻게 바꾸는가?

## 비교 대상

| 항목 | 직접 요청 | Redis 대기열 |
| --- | --- | --- |
| 진입 방식 | `POST /direct` | 대기표 → polling → 입장 토큰 → 구매 |
| 초과 요청 | HTTP 429 | FIFO 대기 후 TTL 만료 시 HTTP 410 |
| 활성 상한 | JVM semaphore | Redis active lease + 동일 JVM semaphore |
| Git commit / tag |  |  |

## 정책과 불변식

- 공정성: 대기표 발급 시 Redis `INCR` sequence와 비공개 입장 token을 함께 저장한다. poll Lua는 요청한 `QUEUED` 대기표가 `ZRANK == 0`이고 active 수가 상한보다 작을 때만 그 대기표 하나를 입장시킨다. Redis script 직렬화 때문에 동시 poll도 정확한 head 순서로 처리되며 다른 대기표를 미리 승격하지 않는다. `lastAdmittedSequence`는 마지막 입장 sequence이며 더 작은 sequence가 뒤늦게 입장할 때만 `fifoViolations`가 증가한다.
- 원자성: 발급, 만료 정리와 요청 head 입장, 입장 토큰 claim, slot release는 각각 Lua script 한 번으로 수행한다. queue·ticket·admission·active key는 모두 `{waiting-room}` Redis hash tag를 공유한다.
- 만료: 기본 대기표 TTL은 30초, 미사용 입장 토큰은 10초, 구매를 시작한 active lease는 5초다. 요청, polling, metrics 조회 시 만료 score를 원자적으로 정리하므로 프로세스가 중단돼도 slot은 영구 점유되지 않는다.
- 우회 방지: 구매는 같은 대기표에 발급된 일회용 token만 `READY → PROCESSING`으로 claim할 수 있다. 살아 있는 대기표에 대한 위조·중복 claim은 HTTP 403, 이미 소비됐거나 만료된 대기표·token은 HTTP 410이다.
- fail-closed: Redis가 응답하지 않으면 대기열 발급·polling·구매·metrics/reset은 HTTP 503으로 중단한다. 비교군인 direct endpoint만 Redis 없이 동작한다.
- reset: `/reset`은 이 lab namespace만 삭제하며 측정 사이에만 호출한다. 실행 중 요청과 동시에 reset하는 동작은 지원하지 않는다.
- 트랜잭션: 이 slice는 JPA 상태를 변경하지 않는다. facade는 RDBMS transaction을 열지 않고 Redis Lua 실행 하나가 각 상태 전이의 원자 경계다.

## API

| Method | Path | 성공 / 제어 상태 |
| --- | --- | --- |
| POST | `/api/labs/waiting-room/direct` | 200 accepted / 429 capacity exceeded |
| POST | `/api/labs/waiting-room/tickets` | 202 queued |
| GET | `/api/labs/waiting-room/tickets/{ticketId}` | 202 queued / 200 admitted / 410 expired |
| POST | `/api/labs/waiting-room/purchase` | 200 accepted / 403 bypass / 410 expired |
| GET | `/api/labs/waiting-room/metrics` | downstream 및 waiting-room counters |
| POST | `/api/labs/waiting-room/reset` | lab Redis key와 counters 초기화 |

구매 요청 본문은 다음과 같다.

```json
{
  "ticketId": "대기표 UUID",
  "admissionToken": "polling에서 받은 UUID"
}
```

## 기본 설정

| 환경 변수 | 기본값 |
| --- | ---: |
| `WAITING_ROOM_MAX_CONCURRENCY` | `4` |
| `WAITING_ROOM_WORK_DURATION` | `50ms` |
| `WAITING_ROOM_TICKET_TTL` | `30s` |
| `WAITING_ROOM_ADMISSION_TTL` | `10s` |
| `WAITING_ROOM_PROCESSING_LEASE_TTL` | `5s` |
| `WAITING_ROOM_POLL_INTERVAL` (최대 advice) | `1s` |

Redis image는 비교 재현성을 위해 `redis:7.4.2-alpine`으로 고정한다.

## 정확한 실행 순서

저장소 루트에서 실행한다. 두 모드는 동시에 실행하지 않는다.

```bash
docker compose config
docker compose up --build --detach
curl --fail --request POST http://localhost:8080/api/labs/waiting-room/reset
```

직접 요청 비교군:

```bash
export BASE_URL=http://localhost:8080
export RESULTS_DIR="$PWD/load-tests/results"
export RESULT_LABEL="waiting-room-direct-$(git rev-parse --short HEAD)"
export WAITING_ROOM_MODE=direct
bash load-tests/capture-environment.sh
k6 run load-tests/scenarios/flash-sale-admission.js
curl --fail http://localhost:8080/api/labs/waiting-room/metrics \
  > "$RESULTS_DIR/$RESULT_LABEL/backend-metrics.json"
```

Redis 대기열 실험군:

```bash
curl --fail --request POST http://localhost:8080/api/labs/waiting-room/reset
export RESULT_LABEL="waiting-room-queued-$(git rev-parse --short HEAD)"
export WAITING_ROOM_MODE=queued
bash load-tests/capture-environment.sh
k6 run load-tests/scenarios/flash-sale-admission.js
curl --fail http://localhost:8080/api/labs/waiting-room/metrics \
  > "$RESULTS_DIR/$RESULT_LABEL/backend-metrics.json"
docker compose down
```

두 실행은 같은 `WAITING_ROOM_*` 도착률, CPU/memory 제한, warm-up 조건을 사용한다. direct 기본 VU는 200/최대 1,000이고, queued iteration은 대기표 TTL까지 VU를 점유할 수 있어 3,000/최대 4,000을 사용한다. 부하 발생기 메모리가 부족하면 `WAITING_ROOM_PEAK_RATE`, `WAITING_ROOM_PRE_ALLOCATED_VUS`, `WAITING_ROOM_MAX_VUS`, ticket TTL을 함께 낮춘다. `dropped_iterations`에는 `count==0` threshold가 있으며, 발생한 실행은 자동 실패하고 결과에서 제외한다.

동일 조건 3회 비교와 결과 수집은 runner로 한 번에 재현할 수 있다. 시간 순서 편향을 줄이기 위해 홀수 run은 direct→queued, 짝수 run은 queued→direct로 실행한다.

```bash
export COMPOSE_PROJECT_NAME=waitinglab
export RESULTS_DIR="$PWD/docs/reports/raw/waiting-room"
export WAITING_ROOM_RUNS=3
bash load-tests/run-waiting-room-lab.sh
```

## 수집 지표

- k6: `waiting_room_accepted_total`, `waiting_room_queued_total`, `waiting_room_rejected_total`, `waiting_room_wait_duration`, 성공한 구매의 `waiting_room_accepted_end_to_end_duration`
- backend: downstream/waiting-room `maxActive`, `maxQueueDepth`, `fifoViolations`, 만료·우회 거절·완료 counters

## 결과

| 지표 | 직접 요청 | Redis 대기열 | 변화 |
| --- | ---: | ---: | ---: |
| accepted |  |  |  |
| queued |  |  |  |
| rejected |  |  |  |
| wait duration p50 / p95 / p99 |  |  |  |
| backend max active |  |  |  |
| backend max queue depth |  |  |  |
| FIFO violations |  |  |  |
| expired tickets / admissions |  |  |  |
| bypass rejected |  |  |  |

## 해석과 한계

- 가설 채택 / 기각:
- 사용자 지표와 backend counter를 함께 본 원인:
- 단일 Redis node 및 단일 애플리케이션 인스턴스라는 한계:
- 다음 실험:

이 lab의 downstream은 실제 주문·재고를 변경하지 않는 50ms 제어 작업이다. Redis release가 downstream 완료 뒤 실패하면 호출자는 503을 받더라도 작업은 이미 수행됐을 수 있으며, processing lease가 만료될 때 slot만 복구된다. 실제 주문 통합에서는 별도의 idempotency와 완료 기록이 필요하다. 또한 처리 시간이 5초 lease를 넘으면 만료 정리가 새 slot을 열 수 있으므로, 운영 구성에서는 최악 처리 시간보다 충분히 긴 lease와 갱신 전략이 필요하다.

poll은 요청 대기표가 현재 queue head이고 빈 slot이 있을 때만 그 대기표 하나를 입장시키며, 같은 HTTP 응답에서 예약 token을 반환한다. 따라서 아직 token을 회수하지 않은 다른 대기표가 admission slot을 미리 점유하지 않는다. 반대로 head 사용자가 polling을 중단하면 ticket TTL 만료 전까지 뒤 사용자를 막을 수 있다. 이는 strict FIFO를 유지하기 위해 의도한 tradeoff이며, 운영형 설계에서는 명시적 취소와 SSE/push admission을 함께 검토해야 한다.

`WAITING_ROOM_POLL_INTERVAL`은 고정 주기가 아니라 최대 advice다. 응답의 `pollAfterMillis`는 `batchesAhead = floor((position - 1) / maxConcurrency)`, `floor = max(1ms, workDuration / 5)`, `estimate = batchesAhead × workDuration`, `advice = min(maxPollInterval, max(floor, estimate / 2))`로 계산한다. 기본값에서는 position 1~4가 10ms, 5~8이 25ms이고 먼 대기표는 최대 1초다. admitted 응답은 더 polling하지 않도록 0을 반환한다.

이 방식은 모든 사용자의 고정 100ms polling이 만든 약 5,000 HTTP requests/s 증폭을 피하면서, head poll과 token 전달 사이에 admission slot이 놀지 않게 한다. client는 최초 발급값을 계속 재사용하지 않고 **매 queued 응답의 최신 `pollAfterMillis`로 다음 sleep을 교체**해야 한다. 대신 head 주변 요청은 1초 고정보다 많아지고 계산은 현재 position과 50ms 처리시간을 이용한 근사치이므로, 실제 wait duration과 HTTP 요청 수를 함께 측정해야 한다. client가 advice보다 자주 polling하지 않는다는 전제도 필요하다.

측정 전 문서이므로 결과와 결론은 비워 둔다.
