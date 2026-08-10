# Lab: cache stampede

- 상태: 측정 대기
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

측정값과 결론은 실행 후에만 기록한다. 아래 결과표는 의도적으로 비워 두었다.

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

| 사용자 지표 | main no-cache | naive | protected |
| --- | ---: | ---: | ---: |
| 응답 시간 p50 |  |  |  |
| 응답 시간 p95 |  |  |  |
| 응답 시간 p99 |  |  |  |
| 성공 RPS |  |  |  |
| 오류율 |  |  |  |
| dropped iterations |  |  |  |

| 백엔드 지표 | main no-cache | naive | protected |
| --- | ---: | ---: | ---: |
| requests |  |  |  |
| cache hits |  |  |  |
| cache misses |  |  |  |
| origin loads |  |  |  |
| lock contention |  |  |  |
| JVM CPU / heap / GC |  |  |  |

## 해석

- 가설 채택/기각:
- origin load 감소와 p95/p99의 관계:
- lock contention 및 timeout 비용:
- Redis 장애와 lease 만료에서 관찰한 동작:
- 다음 실험:

## 한계

- 로컬 단일 Redis/단일 backend 구성이며 Redis 자체의 failover는 검증하지 않는다.
- 제어된 40ms 지연은 재현 장치이고 실제 DB latency 분포가 아니다.
- TTL 동시 만료와 hot key 하나만 다루며 key cardinality가 큰 cache 운영 비용은 측정하지 않는다.
- 결과를 채우기 전까지 구현의 성능 우위나 origin load 감소를 결론 내리지 않는다.
