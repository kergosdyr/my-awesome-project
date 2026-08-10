# 커머스 실험용 k6 하네스

이 디렉터리는 같은 트래픽을 기준선과 실험군에 반복해 적용하고, 실제 k6 측정 결과를 원본 JSON과 사람이 읽는 텍스트로 남긴다. 도착률(`ramping-arrival-rate`)을 고정하므로 서버가 느려져도 요청 생성률이 저절로 낮아지지 않는다. `dropped_iterations`가 생기면 부하 발생기 자체가 목표 도착률을 못 만든 것이므로 해당 실행은 비교 결과에서 제외하거나 더 강한 부하 발생기에서 다시 실행한다.

## 시나리오

| 파일 | 기본 대상 | 목적 | 기본 부하 |
| --- | --- | --- | --- |
| `scenarios/catalog-read-baseline.js` | `GET /api/products` | 카탈로그 읽기 기준선 | 5 → 50 RPS, 30초 상승 + 2분 유지 + 30초 하강 |
| `scenarios/hot-product-burst.js` | `GET /api/products/1` | 캐시 스탬피드 전후 비교 | 20 → 250 RPS, 10초 상승 + 45초 유지 + 10초 하강 |
| `scenarios/order-throughput.js` | `POST /api/orders` | 동기 처리와 Kafka/outbox 처리 비교 | 1 → 2 RPS, 15초 상승 + 30초 유지 + 15초 하강 |
| `scenarios/flash-sale-admission.js` | `/api/labs/waiting-room` | direct 429와 queued E2E 흐름 비교 | 10 → 100 RPS, 15초 상승 + 1분 유지 + 15초 하강 |

각 시나리오는 상태와 응답 형태를 `check`로 검증하고, 시나리오 전용 성공·오류 횟수와 비율을 기록한다. 기본 임계치는 오류율 1% 미만과 시나리오별 p95/p99 응답 시간이다. 대기열 시나리오는 `WAITING_ROOM_MODE=direct|queued`로 두 실행을 분리한다. direct의 429는 예상된 용량 제어 결과이며, queued는 대기표 발급부터 입장 후 구매까지 한 iteration으로 측정한다.

## 실행 전 준비

k6 0.56 이상과 실행 중인 백엔드가 필요하다. 저장소 루트에서 다음처럼 환경을 고정한다.

```bash
export BASE_URL=http://localhost:8080
export RESULTS_DIR="$PWD/load-tests/results"
export RESULT_LABEL="catalog-baseline-$(git rev-parse --short HEAD)"
bash load-tests/capture-environment.sh
```

환경 캡처 스크립트가 `$RESULTS_DIR/$RESULT_LABEL`을 먼저 만들고 Git SHA, OS, CPU, 메모리, k6/JVM/Docker 버전을 `environment.txt`에 기록한다. k6의 `handleSummary`는 같은 디렉터리에 원본 `summary.json`과 `summary.txt`를 쓴다. 결과 파일에 비밀값은 넣지 않는다.

MySQL fixture는 매 비교 전에 같은 상태로 되돌린다. 주문 테스트의 예상 성공 요청 수보다 상품 재고가 많아야 한다. 기본 seed의 상품 1 재고는 120개이므로 더 높은 주문 RPS나 긴 실행 시간을 사용할 때는 전용 상품을 충분한 재고로 준비하고 `ORDER_PRODUCT_ID`로 지정한다. 기준선만 캐시가 차 있거나 실험군만 DB 버퍼가 따뜻한 상태가 되지 않도록 warm/cold 조건을 명시한다.

## 워밍업과 본 측정

워밍업은 결과 비교에서 제외한다. 아래처럼 낮은 도착률로 JVM, 커넥션 풀, DB 버퍼와 의도한 캐시를 준비한다.

```bash
export RESULT_LABEL="warmup-$(git rev-parse --short HEAD)"
bash load-tests/capture-environment.sh
CATALOG_READ_START_RATE=2 \
CATALOG_READ_PEAK_RATE=10 \
CATALOG_READ_RAMP_UP=10s \
CATALOG_READ_STEADY=20s \
CATALOG_READ_RAMP_DOWN=10s \
k6 run load-tests/scenarios/catalog-read-baseline.js
```

본 측정은 새로운 라벨로 실행한다.

```bash
export RESULT_LABEL="catalog-baseline-$(git rev-parse --short HEAD)"
bash load-tests/capture-environment.sh
k6 run load-tests/scenarios/catalog-read-baseline.js

export RESULT_LABEL="stampede-baseline-$(git rev-parse --short HEAD)"
bash load-tests/capture-environment.sh
k6 run load-tests/scenarios/hot-product-burst.js

export RESULT_LABEL="order-baseline-$(git rev-parse --short HEAD)"
bash load-tests/capture-environment.sh
k6 run load-tests/scenarios/order-throughput.js

export RESULT_LABEL="waiting-room-baseline-$(git rev-parse --short HEAD)"
export WAITING_ROOM_MODE=direct
bash load-tests/capture-environment.sh
k6 run load-tests/scenarios/flash-sale-admission.js
```

대기열 실험군은 `/api/labs/waiting-room/reset`을 호출한 뒤 `WAITING_ROOM_MODE=queued`와 새 `RESULT_LABEL`로 같은 시나리오를 다시 실행한다. 상세 순서는 [`../docs/labs/waiting-room.md`](../docs/labs/waiting-room.md)에 고정한다.

## 부하와 임계치 조정

각 시나리오는 접두사별 환경 변수로 같은 구조를 조정한다. 예를 들어 hot-product 실험은 다음 값을 사용한다.

```bash
HOT_PRODUCT_START_RATE=20
HOT_PRODUCT_PEAK_RATE=400
HOT_PRODUCT_RAMP_UP=15s
HOT_PRODUCT_STEADY=2m
HOT_PRODUCT_RAMP_DOWN=15s
HOT_PRODUCT_PRE_ALLOCATED_VUS=200
HOT_PRODUCT_MAX_VUS=800
HOT_PRODUCT_MAX_ERROR_RATE=0.01
HOT_PRODUCT_P95_MS=500
HOT_PRODUCT_P99_MS=1000
HOT_PRODUCT_ID=1
```

다른 접두사는 `CATALOG_READ`, `ORDER`, `WAITING_ROOM`이다. 공통 HTTP timeout은 `REQUEST_TIMEOUT`으로 바꾼다. 주문 payload는 `{customerName, lines:[{productId, quantity}]}`이며 `ORDER_PRODUCT_ID`, `ORDER_QUANTITY`, `ORDER_CUSTOMER_PREFIX`로 조정한다. 대기열은 `WAITING_ROOM_MODE=direct|queued`를 지원하고 `WAITING_ROOM_START_RATE`, `WAITING_ROOM_PEAK_RATE`, 단계 시간과 VU 설정을 두 모드에 동일하게 적용한다.

## 전후 비교 규칙

한 번에 독립 변수 하나만 바꾸고, 다음 조건을 기준선과 실험군에 동일하게 유지한다.

- 같은 머신/컨테이너 CPU·메모리 제한, JVM 옵션, k6 버전, 네트워크 경로
- 같은 MySQL/Redis/Kafka 버전과 설정, 데이터 건수·분포·인덱스, 주문 시작 재고
- 같은 warm-up, 도착률 단계, 지속 시간, 최대 VU, timeout과 성공 상태 정의
- 같은 관측 수집 간격과 백그라운드 프로세스 조건
- 각 군 최소 3회 반복 후 중앙값 비교; 첫 실행만 임의로 선택하지 않기
- 실행 사이 주문·outbox·consumer offset·Redis key를 의도한 fixture로 복구

애플리케이션 효과는 k6 지표만으로 결론 내리지 않는다. 같은 시간 창에서 CPU, heap/GC pause, DB QPS와 active connection, 쿼리 실행 횟수, cache hit/miss와 실제 loader 호출 횟수, Redis QPS, Kafka produce latency·consumer lag, outbox backlog, 대기열 허용/대기/제한 건수를 함께 기록한다. 캐시 스탬피드는 TTL 만료 직후를 재현하고 DB 조회 횟수가 요청 수에 비례하는지 확인한다. Kafka/outbox는 broker 지연·중단을 주입한 뒤 유실, 중복, backlog 회복도 검증한다. 대기열은 downstream 처리 한도를 넘는 입력에서 실제 주문 수가 재고와 허용량을 위반하지 않는지 검증한다.

결과는 [`../docs/labs/_template.md`](../docs/labs/_template.md)에 옮기고 기준선/실험군의 Git commit 또는 tag와 원본 `summary.json`, `summary.txt`, `environment.txt` 경로를 링크한다. 수집하지 못한 값은 추정해서 채우지 말고 미수집 사유를 남긴다.
