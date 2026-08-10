# Lab: Redis FIFO 대기열

- 상태: 완료
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
| Git commit / tag | `0e9dcb1` | `0e9dcb1` |

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

2026-08-10 UTC에 commit `0e9dcb1`의 direct/queued를 각각 3회 완주했다.
아래 값은 sample을 합친 percentile이 아니라 **각 run 값 3개의 중앙값**이다.
원시 summary, backend metrics, 환경과 제외 근거는
[raw evidence README](../reports/raw/waiting-room/README.md)에 보존했다.

| 지표 | 직접 요청 | Redis 대기열 | 변화 |
| --- | ---: | ---: | ---: |
| accepted | 5,509 | 7,574 | +2,065 (+37.5%) |
| rejected | 2,065 | 0 | -2,065 (-100%) |
| accepted / 전체 flow | 72.74% | 100% | +27.26%p |
| accepted E2E p50 / p95 / p99 | 52 / 60 / 61ms | 14,054.5 / 27,880.8 / 28,255ms | p50 +14,002.5ms |
| queue wait p50 / p95 / p99 | 해당 없음 | 14,003 / 27,828.8 / 28,202ms | 대기 비용 추가 |
| 전체 HTTP requests | 7,954 | 191,071 | 약 24.0배 |
| peak VU | 5 | 1,962 | 약 392배 |
| downstream max active | 4 | 4 | 상한 유지 |
| backend max queue depth | 0 | 1,960 | +1,960 |
| FIFO violations | 해당 없음 | 0 | 위반 없음 |
| expired tickets / admissions | 0 / 0 | 0 / 0 | 만료 없음 |
| lab errors / dropped iterations | 0 / 0 | 0 / 0 | 모두 완주 |

direct run별 accepted/rejected는 5,505/2,070, 5,509/2,065,
5,518/2,057이었고 queued는 세 번 모두 7,574/0이었다. direct의 HTTP 429는
의도한 제어 결과라 k6 `http_req_failed`에는 잡히지만 lab error에는 포함하지
않는다. queued의 191,071은 ticket 발급, poll, 구매, metrics sampling을 합친
전체 HTTP 수이며 정확한 poll-only counter가 아니다.

## 실패 정책 검증

| 시나리오 | 관측 | 판정 |
| --- | --- | --- |
| Redis 중단 | ticket 발급 503, metrics 503, Redis 비의존 direct 구매 200 | queue는 fail-closed, 비교군은 독립 동작 |
| token 위조 / replay | 정상 구매 200, 위조 token 403, 소비 token replay 410 | bypass와 재사용 차단 |
| admission TTL | active 4에서 5번째 poll 202, 11초 뒤 200, 첫 만료 token 구매 410 | 4개 만료 slot 회수 후 FIFO 재입장 |

정확한 admission TTL 재현의 중간 metrics는 `currentActive=4`, `maxActive=4`,
최종 metrics는 `expiredAdmissions=4`, `currentActive=1`,
`lastAdmittedSequence=5`, `fifoViolations=0`이었다. 각 요청의 curl trace,
응답 body와 HTTP status는
[`failure-admission-expiry-0e9dcb1`](../reports/raw/waiting-room/failure-admission-expiry-0e9dcb1/)에
있다. admission을 2개만 활성화했던 기존 결과는 삭제하지 않고
`failure-admission-expiry-invalid-preflight-0e9dcb1`로 이름을 바꿔 제외했다.

## 해석과 한계

가설은 부분 채택한다. Redis 대기열은 downstream 동시 처리 상한 4와 FIFO를
지키면서 direct의 중앙값 2,065건 HTTP 429를 0으로 만들었다. 다만 처리 용량을
높인 것이 아니라 거절을 대기로 전환한 결과다. 동일 도착 프로파일에서 direct는
약 90.067초에 끝났지만 queued는 남은 대기열을 비우느라 약 113.161초가
걸렸다. 따라서 accepted +37.5%를 throughput 향상으로 해석하면 안 된다.

그 대가는 중앙값 14.003초의 queue wait, p99 28.202초, 전체 HTTP 약 24배,
peak VU 1,962와 max queue depth 1,960이다. queued p99 대기는 30초 ticket TTL과
약 1.8초 차이뿐이어서 처리시간이나 부하가 조금만 증가해도 만료가 생길 수 있다.
최종 3회에는 만료·downstream rejection·FIFO 위반·drop이 없었지만, 이 안정성은
현재 50ms 작업과 100 RPS 프로파일 범위의 관측이다.

최종 비교에서 제외한 `14bc9ed`/`c6d1237` preflight는 중단, incomplete run,
error 또는 dropped iteration이 있어 개선율 계산에 사용할 수 없다. 특히
`14bc9ed` queued preflight의 473,624는 정확한 poll 횟수가 아니라 ticket,
poll, purchase, metrics 요청을 모두 합친 `http_reqs`다. 실패한 설계의 poll
amplification을 보여 주는 방향성 증거로만 남긴다.

실험은 Apple M4 단일 호스트, 단일 애플리케이션 인스턴스와 단일 Redis node에서
각 모드 3회만 수행했다. 저장된 `environment.txt`의 `[compose-services]`가 비어
있어 해당 파일만으로 실행 시점의 container CPU/memory 제한 적용 여부를
독립 입증할 수 없다. 실제 주문·재고·DB lock, 다중 인스턴스, Redis failover와
network partition도 포함하지 않았다. 다음 실험에서는 operation별 poll counter,
더 긴 steady state, ticket TTL 여유, SSE/push 또는 취소 가능한 queue를 비교해야
한다.

이 lab의 downstream은 실제 주문·재고를 변경하지 않는 50ms 제어 작업이다. Redis release가 downstream 완료 뒤 실패하면 호출자는 503을 받더라도 작업은 이미 수행됐을 수 있으며, processing lease가 만료될 때 slot만 복구된다. 실제 주문 통합에서는 별도의 idempotency와 완료 기록이 필요하다. 또한 처리 시간이 5초 lease를 넘으면 만료 정리가 새 slot을 열 수 있으므로, 운영 구성에서는 최악 처리 시간보다 충분히 긴 lease와 갱신 전략이 필요하다.

poll은 요청 대기표가 현재 queue head이고 빈 slot이 있을 때만 그 대기표 하나를 입장시키며, 같은 HTTP 응답에서 예약 token을 반환한다. 따라서 아직 token을 회수하지 않은 다른 대기표가 admission slot을 미리 점유하지 않는다. 반대로 head 사용자가 polling을 중단하면 ticket TTL 만료 전까지 뒤 사용자를 막을 수 있다. 이는 strict FIFO를 유지하기 위해 의도한 tradeoff이며, 운영형 설계에서는 명시적 취소와 SSE/push admission을 함께 검토해야 한다.

`WAITING_ROOM_POLL_INTERVAL`은 고정 주기가 아니라 최대 advice다. 응답의 `pollAfterMillis`는 `batchesAhead = floor((position - 1) / maxConcurrency)`, `floor = max(1ms, workDuration / 5)`, `estimate = batchesAhead × workDuration`, `advice = min(maxPollInterval, max(floor, estimate / 2))`로 계산한다. 기본값에서는 position 1~4가 10ms, 5~8이 25ms이고 먼 대기표는 최대 1초다. admitted 응답은 더 polling하지 않도록 0을 반환한다.

client는 최초 발급값을 계속 재사용하지 않고 **매 queued 응답의 최신
`pollAfterMillis`로 다음 sleep을 교체**해야 한다. position-aware advice는 실패한
고정 100ms preflight보다 polling을 줄이기 위한 것이지만, 최종 queued도 direct보다
전체 HTTP가 약 24배 많았다. 운영형 설계에서는 client가 advice보다 자주 poll하지
않도록 하고 서버에서 polling rate와 queue age를 직접 관측해야 한다.
