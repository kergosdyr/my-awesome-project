# Cache stampede main baseline

## 요약

`main`의 상품 상세 API는 캐시 없이 요청마다 MySQL을 조회합니다. 짧은 로컬 부하 프로파일을 3회 반복했을 때 오류는 없었고, p95 중앙값은 **2.12ms**, p99 중앙값은 **4.63ms**, 평균 도착 처리량 중앙값은 **168.29 req/s**였습니다.

이 수치는 캐시의 필요성을 입증하지 않습니다. 현재 seed 6개와 단일 인덱스 조회는 이미 매우 작고 빠릅니다. cache lab에서는 의도적으로 느린 origin과 TTL 동시 만료를 추가해 stampede 메커니즘을 재현하고, latency보다 **origin loader 호출 수가 요청 수에 비례하는지**를 우선 비교합니다.

## 측정 정의

- 기준 commit: `aa49aeb` (`baseline/v1`)
- API: `GET /api/products/1`
- 부하: 20 → 200 RPS, 5초 상승 + 20초 유지 + 5초 하강
- 실행: 워밍업 1회 제외, 본 측정 3회
- 애플리케이션 제한: backend 1 CPU / 768MiB, MySQL 1 CPU / 1GiB
- 부하 발생기: 같은 호스트의 k6 v0.56.0
- 성공: HTTP 200, JSON object, product id 일치

## 반복 결과

| 실행 | 요청 수 | 평균 처리량(req/s) | p50(ms) | p95(ms) | p99(ms) | 오류율 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| run 1 | 5,049 | 168.292 | 1.193 | 2.485 | 5.265 | 0% |
| run 2 | 5,049 | 168.290 | 1.362 | 2.119 | 4.630 | 0% |
| run 3 | 5,050 | 168.303 | 1.347 | 1.888 | 3.984 | 0% |
| 중앙값 | — | **168.292** | **1.347** | **2.119** | **4.630** | **0%** |

MySQL digest counter는 워밍업을 포함한 15,948개 HTTP 요청 동안 상품 상세 SELECT 15,947회를 기록했습니다. 관측된 1회 차이는 보정하지 않았습니다. 본 측정 3회의 요청 수는 15,148개이며 코드의 명시적 fetch plan은 요청당 상세 SELECT 1회입니다.

## 원본 증거

- [run 1 summary](raw/cache-stampede/main-no-cache-run1/summary.json)
- [run 2 summary](raw/cache-stampede/main-no-cache-run2/summary.json)
- [run 3 summary](raw/cache-stampede/main-no-cache-run3/summary.json)
- [backend counters](raw/cache-stampede/main-no-cache-backend-metrics.txt)

각 run 디렉터리의 `environment.txt`에는 OS, CPU, memory, Java, Docker, Compose 상태가 포함됩니다.

## 한계

- 단일 Apple Silicon 호스트, 단일 애플리케이션·MySQL 인스턴스의 30초 측정입니다.
- 데이터셋과 query가 작아 DB buffer에 들어갑니다.
- CPU, heap, GC의 시계열은 아직 수집하지 않았습니다.
- 이 결과는 운영 용량이나 캐시 도입 효과를 일반화하지 않습니다.
