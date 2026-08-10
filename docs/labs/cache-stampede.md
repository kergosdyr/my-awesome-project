# Lab: cache stampede

- 상태: 로컬 측정 완료 (2026-08-11 KST)
- 브랜치: `lab/cache-stampede`
- 추적 Issue: [#1 Cache stampede lab](https://github.com/kergosdyr/my-awesome-project/issues/1)
- 기준선: `c691359`의 [main 측정 보고서](../reports/cache-stampede-main-baseline.md)

## 질문

짧은 TTL의 hot-product cache가 동시에 만료될 때, Redis 분산 잠금과 double-check가 요청 오류를 늘리지 않으면서 MySQL origin load의 중복을 억제하는가?

## 가설과 판정 기준

같은 TTL, origin 지연, 상품, 도착률에서 naive cache-aside는 만료 직후 동시 miss 수만큼 origin load가 증가한다. Protected 전략은 한 프로세스가 아니라 Redis 잠금 소유자 한 명만 origin을 읽고, 나머지 요청은 채워진 cache를 사용한다.

판정은 latency만으로 하지 않는다. 각 실행에서 다음을 함께 본다.

- `requests`, `hits`, `misses`, `originLoads`, `lockContention`
- k6 성공률, p50/p95/p99, 처리량, `dropped_iterations`
- 같은 시간대의 `http_server_requests_seconds`, JVM/프로세스 지표
- Redis 장애, 잠금 timeout, lease 만료 시 HTTP 결과와 origin fallback 여부

실측 결과, protected 전략은 오류와 처리량 저하 없이 origin load 중앙값을 985회에서 119회로 **87.9% 줄였다**. p95는 42.32ms에서 24.27ms로 **42.7% 낮아졌지만**, p99 개선은 1.7%에 그쳤고 한 실행의 최대 지연은 오히려 92.23ms까지 늘었다. 따라서 이 구현은 DB 증폭 억제에는 효과가 있었지만 모든 tail latency를 제거하지는 않았다.

## 비교 대상

| 전략 | API | cache miss 동작 | 동시 갱신 제어 |
| --- | --- | --- | --- |
| main no-cache | `GET /api/products/1` | 요청마다 MySQL 조회 | 없음 |
| naive | `GET /api/labs/cache/naive/products/1` | origin 조회 후 Redis 저장 | 없음 |
| protected | `GET /api/labs/cache/protected/products/1` | 잠금 획득, double-check, origin 조회 후 저장 | Redis `SET NX PX` + token Lua unlock |

Naive와 protected cache key는 서로 분리되어 한 전략의 warm cache가 다른 전략 결과에 섞이지 않는다. 기존 `/api/products`와 `/api/orders` 계약은 바꾸지 않는다.

## 설계와 소유권

```text
api.cache.CacheExperimentController
  -> domain.cache.CacheExperimentService
       -> domain.product.ProductReader        (origin)
       -> domain.cache.ProductCache           (port)
       -> domain.cache.CacheLock              (port)
       -> domain.cache.CacheExperimentMetrics
       -> domain.cache.ExperimentDelay        (port)

infra.storage.redis.cache.RedisProductCache   -> StringRedisTemplate + JSON payload
infra.storage.redis.cache.RedisCacheLock      -> SET NX PX + token-check Lua unlock
infra.config                                  -> typed policy + controlled Thread delay
```

Controller는 `CacheExperimentService` 하나만 호출한다. HTTP path와 응답 shape은 `api`, cache/lock 계약과 전략/결과/지표 의미는 `domain`, Redis key·JSON·command·Lua는 `infra`가 소유한다.

### Naive cache-aside

1. 전략별 cache key를 읽는다.
2. hit이면 SQL 없이 반환한다.
3. miss이면 `originDelay`만큼 대기한 뒤 상품을 한 번 조회한다.
4. 상품 snapshot을 TTL과 함께 Redis에 저장한다.

만료 직후 여러 요청이 2번 단계에 함께 도달하면 각 요청이 origin을 읽을 수 있다. 이것이 이 전략에서 의도적으로 관찰할 stampede이다.

### Protected double-check

1. 전략별 cache key를 읽는다.
2. miss이면 상품별 Redis lock을 `SET NX PX`로 시도한다.
3. lock owner는 cache를 다시 확인한다. 여전히 miss일 때만 origin을 한 번 읽고 cache를 채운다.
4. waiter는 짧게 polling하며 채워진 값을 읽거나, 이전 owner가 끝난 뒤 lock을 획득하면 다시 double-check한다.
5. owner token이 현재 lock value와 같은 경우에만 Lua script가 lock을 삭제한다.

대기는 `lockWaitTimeout`으로 제한한다. timeout 시 보호를 우회해 origin을 읽지 않고 `503 CACHE_LOCK_TIMEOUT`을 반환한다.

## 설정

| 환경 변수 | 기본값 | 의미 |
| --- | ---: | --- |
| `CACHE_LAB_TTL` | `500ms` | 두 전략 cache entry TTL |
| `CACHE_LAB_ORIGIN_DELAY` | `40ms` | miss마다 origin 조회 전에 넣는 제어 지연 |
| `CACHE_LAB_LOCK_LEASE` | `2s` | protected lock 자동 만료 |
| `CACHE_LAB_LOCK_WAIT_TIMEOUT` | `1s` | waiter의 최대 polling 시간 |
| `CACHE_LAB_LOCK_POLL_INTERVAL` | `10ms` | waiter polling 간격 |

기본 TTL은 45초 steady 구간 동안 여러 번 만료되며, 40ms 지연은 작은 seed/로컬 MySQL에서도 동시 miss를 재현하기 위한 실험 장치다. 운영 권장값이 아니다. Lock lease는 origin delay보다 길어야 애플리케이션이 시작되지 않는다.

## API와 지표 정의

```http
GET  /api/labs/cache/naive/products/{productId}
GET  /api/labs/cache/protected/products/{productId}
GET  /api/labs/cache/metrics
POST /api/labs/cache/reset
```

상품 응답은 기존 envelope 안에 `strategy`, `outcome`, `product`를 제공한다. `outcome`은 다음 중 하나다.

- `CACHE_HIT`: 최초 cache lookup hit
- `ORIGIN_LOADED`: 이 요청이 origin을 읽고 cache를 채움
- `PEER_FILLED`: 최초 miss 뒤 lock owner 또는 앞선 요청이 채운 값을 사용

지표는 프로세스 로컬이며 전략별 의미는 다음과 같다.

- `requests`: facade에 진입한 요청
- `hits`: 최초 lookup hit
- `misses`: 최초 lookup miss
- `originLoads`: 제어 지연과 실제 product origin read를 시작한 횟수
- `lockContention`: protected 요청이 최초 lock 획득에 실패한 횟수; naive는 항상 0

`hits + misses`는 Redis 읽기 자체가 실패하지 않은 요청에서 `requests`와 같다. Redis 오류가 lookup 도중 발생하면 요청만 증가할 수 있으므로 이 차이도 장애 신호로 남긴다.

Reset은 `commerce-lab:cache-stampede:cache:*` namespace만 `SCAN`하고 `UNLINK`한다. `FLUSHALL`이나 다른 기능의 key 삭제는 하지 않는다. 진행 중인 owner의 lock도 삭제하지 않는다. 지표는 새 counter set으로 원자적으로 교체한다. 비교 실행 사이 트래픽이 없는 상태에서만 호출해야 경계가 명확하다.

## 트랜잭션과 fetch/query 계획

`CacheExperimentService.readProduct`가 read-only JPA 트랜잭션 owner이고 `ProductReader`가 같은 기본 transaction manager에 참여한다. Cache hit와 waiter의 peer-filled 응답은 Redis만 사용하며 SQL은 0회다. Origin load만 연관관계 없는 `ProductEntity`를 ID로 1회 조회한다. 제어 지연은 SQL 이전에 적용한다.

| 경로 | 예상 MySQL query | 대표 Redis 명령 |
| --- | ---: | --- |
| naive hit | 0 | `GET` |
| naive miss | 1 | `GET`, `SET PX` |
| protected hit | 0 | `GET` |
| protected owner miss | 1 | `GET`, `SET NX PX`, double-check `GET`, `SET PX`, Lua unlock |
| protected waiter | 0 또는 lease 만료 후 1 | polling `GET`, lock 재시도, 필요 시 Lua unlock |

Cache에는 association 없는 product root의 immutable snapshot만 저장한다. API mapper는 이 snapshot만 읽으므로 OSIV가 꺼진 상태에서 lazy load가 없다.

## 정확성 및 실패 정책

- Redis 연결/명령 실패: lab API는 `503 CACHE_UNAVAILABLE`로 fail closed한다. DB fallback으로 Redis 장애가 DB stampede로 바뀌는 것을 숨기지 않는다. 기존 상품/주문 API는 Redis를 사용하지 않는다.
- Lock timeout: `503 CACHE_LOCK_TIMEOUT`이며 보호 없는 origin fallback은 없다.
- Owner 안전성: lease가 만료된 이전 owner는 token-check Lua 때문에 새 owner의 lock을 삭제할 수 없다.
- Lease 한계: 실제 origin 시간이 lease를 넘으면 다음 owner가 생겨 origin load가 중복될 수 있다. Watchdog은 이번 실험 범위가 아니며 `originLoads`로 관찰한다.
- 잘못된 cache JSON: 해당 lab cache key만 `UNLINK`하고 miss로 처리해 origin에서 복구한다.
- Not found: origin의 `PRODUCT_NOT_FOUND`를 그대로 반환하고 negative caching은 하지 않는다.
- Staleness: 주문의 재고 변경은 lab cache를 즉시 무효화하지 않는다. 최대 TTL만큼 `stockQuantity`가 오래될 수 있으므로 측정 중에는 주문 트래픽을 섞지 않는다.
- 다중 backend: lock은 Redis에 분산되지만 실험 counter는 각 JVM 로컬이다. 현재 compose의 backend 1개 조건에서 측정하며 수평 확장 시 외부 metrics aggregation이 필요하다.

## 정확한 실행 절차

이 브랜치에서 설정과 테스트를 먼저 검증한다.

```bash
docker compose config --quiet
cd backend && ./mvnw verify && cd ..
```

전체 스택을 시작하고 health를 확인한다.

```bash
docker compose up --build --detach
curl --fail http://localhost:8080/actuator/health
```

기본 hot-product 프로파일(20 → 250 RPS, 10초 상승 + 45초 유지 + 10초 하강)을 전략별 3회 실행한다.

```bash
export BASE_URL=http://localhost:8080
export CACHE_LAB_RUNS=3
bash load-tests/run-cache-stampede.sh
```

더 강한 동일 프로파일 비교는 기존 환경 변수를 양쪽 전략에 함께 적용한다.

```bash
HOT_PRODUCT_PEAK_RATE=400 \
HOT_PRODUCT_STEADY=2m \
HOT_PRODUCT_PRE_ALLOCATED_VUS=200 \
HOT_PRODUCT_MAX_VUS=800 \
CACHE_LAB_RUNS=3 \
bash load-tests/run-cache-stampede.sh
```

Runner는 각 run 전에 reset하고 다음 파일을 같은 결과 디렉터리에 남긴다.

```text
docs/reports/raw/cache-stampede/cache-stampede-<strategy>-run<n>-<sha>/
  reset.json
  environment.txt
  summary.json
  summary.txt
  backend-metrics.json
  backend-prometheus.txt
```

k6 threshold가 실패해도 backend metrics를 먼저 수집한 뒤 최종 non-zero로 종료한다. 전체 compose 실행과 실제 수치 측정은 비교 조건을 고정해 순차 수행한다.

## 결과

Naive와 protected는 동일한 commit, 컨테이너 제한, 상품, `20 → 250 RPS`, `10초 상승 + 45초 유지 + 10초 하강` 프로파일로 실행했다. 각 전략은 실행 직전에 cache와 counter를 reset했고 3회 중앙값을 대표값으로 사용했다. 모든 실행에서 약 13,850건을 처리했으며 `dropped_iterations`와 HTTP 오류는 0이었다.

`main` no-cache는 더 짧은 `20 → 200 RPS`, 30초 프로파일이고 40ms 제어 지연도 없으므로 아래 표에서는 환경 맥락만 제공한다. Naive 대비 protected만 직접적인 전후 비교다.

| 사용자 지표 | main no-cache (맥락) | naive 중앙값 | protected 중앙값 | protected 변화 |
| --- | ---: | ---: | ---: | ---: |
| 응답 시간 p50 | 1.347ms | 1.213ms | 1.240ms | +2.2% |
| 응답 시간 p95 | 2.119ms | 42.319ms | 24.265ms | **-42.7%** |
| 응답 시간 p99 | 4.630ms | 46.133ms | 45.370ms | -1.7% |
| 성공 RPS | 168.292 | 212.923 | 213.057 | +0.1% |
| 오류율 | 0% | 0% | 0% | 0.0%p |
| dropped iterations | 0 | 0 | 0 | 동일 |

| 백엔드 지표 | naive 중앙값 | protected 중앙값 | protected 변화 |
| --- | ---: | ---: | ---: |
| requests | 13,850 | 13,849 | -1 |
| 최초 cache hits | 12,865 | 12,801 | -0.5% |
| 최초 cache misses | 985 | 1,048 | +6.4% |
| origin loads | 985 | 119 | **-87.9%** |
| lock contention | 0 | 929 | 보호 동작 관측 |
| miss 중 origin load 비율 | 100.0% | 11.4% | **-88.6%p** |

도착률이 고정된 open-model 테스트이므로 성공 RPS가 같다는 것은 두 전략 모두 요청 스케줄을 소화했다는 뜻이지 protected의 최대 처리 용량이 더 크다는 뜻은 아니다.

### 실행별 결과

| 전략 | 실행 | 요청 | 성공 RPS | p50(ms) | p95(ms) | p99(ms) | max(ms) | origin loads | contention |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| naive | 1 | 13,850 | 212.917 | 1.079 | 42.528 | 46.116 | 62.763 | 982 | 0 |
| naive | 2 | 13,849 | 213.057 | 1.213 | 42.319 | 46.253 | 55.865 | 986 | 0 |
| naive | 3 | 13,850 | 212.923 | 1.214 | 42.319 | 46.133 | 53.805 | 985 | 0 |
| protected | 1 | 13,849 | 213.057 | 1.241 | 24.265 | 45.370 | 92.233 | 119 | 929 |
| protected | 2 | 13,850 | 213.060 | 1.192 | 24.174 | 45.324 | 58.082 | 119 | 933 |
| protected | 3 | 13,849 | 213.057 | 1.240 | 24.342 | 45.542 | 63.102 | 119 | 927 |

각 실행 디렉터리에는 k6 `summary.json`/`summary.txt`, 환경 캡처, lab backend metrics, Prometheus snapshot이 함께 있다.

- [naive run 1](../reports/raw/cache-stampede/cache-stampede-naive-run1-16262c1/summary.json), [run 2](../reports/raw/cache-stampede/cache-stampede-naive-run2-16262c1/summary.json), [run 3](../reports/raw/cache-stampede/cache-stampede-naive-run3-16262c1/summary.json)
- [protected run 1](../reports/raw/cache-stampede/cache-stampede-protected-run1-16262c1/summary.json), [run 2](../reports/raw/cache-stampede/cache-stampede-protected-run2-16262c1/summary.json), [run 3](../reports/raw/cache-stampede/cache-stampede-protected-run3-16262c1/summary.json)

### 실패 정책 점검

- Redis를 중지한 뒤 naive 조회는 `503 CACHE_UNAVAILABLE`을 반환했다. 요청 counter만 1 증가했고 miss와 origin load는 0으로 유지되어 DB fallback이 일어나지 않았다. [응답](../reports/raw/cache-stampede/failure-redis-unavailable-16262c1/response.json), [전후 metrics](../reports/raw/cache-stampede/failure-redis-unavailable-16262c1/after-metrics.json)
- 합성 owner가 product lock을 5초 보유한 상태에서 protected 조회는 1초 뒤 `503 CACHE_LOCK_TIMEOUT`을 반환했다. `misses=1`, `lockContention=1`, `originLoads=0`으로 보호 없는 우회가 없었다. [응답](../reports/raw/cache-stampede/failure-lock-timeout-16262c1/response.json), [metrics](../reports/raw/cache-stampede/failure-lock-timeout-16262c1/backend-metrics.json)

## 해석

- **가설 채택:** 동일 만료·지연 조건에서 Redis lock과 double-check가 중복 origin load를 87.9% 억제했다.
- p95는 42.7% 개선됐지만 p99는 1.7%만 개선됐다. 보호 경로의 waiter도 cache refill을 기다리므로 40ms origin 지연 자체는 상위 tail에 남는다.
- protected run 1의 최대값 92.23ms는 naive의 모든 실행 최대값보다 높았다. 짧은 polling, 스케줄링, 만료 경계가 극단값을 만들 수 있어 평균/백분위 개선을 최대 지연 보장으로 해석하면 안 된다.
- Redis 장애와 lock timeout은 의도대로 503 fail-closed였으며 MySQL로 우회하지 않았다. 반면 lock lease 만료 중복과 Redis 재시작/failover는 이번 실행에서 재현하지 않았다.
- 다음 실험은 TTL jitter, stale-while-revalidate, lock lease보다 긴 origin, 다중 backend에서 같은 `originLoads`/tail을 비교하는 것이다.

## 한계

- 로컬 단일 Redis/단일 backend 구성이며 Redis 자체의 failover는 검증하지 않는다.
- 제어된 40ms 지연은 재현 장치이고 실제 DB latency 분포가 아니다.
- TTL 동시 만료와 hot key 하나만 다루며 key cardinality가 큰 cache 운영 비용은 측정하지 않는다.
- Prometheus는 실행 종료 snapshot만 저장했다. CPU, heap, GC 시계열과 MySQL lock-wait를 전략별로 정량 비교하지 않았다.
- 실행 순서는 naive 3회 후 protected 3회로 고정되어 시간 순서 효과를 무작위화하지 않았다.
