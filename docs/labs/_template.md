# Lab: 실험 제목

- 상태: 제안 / 진행 중 / 완료
- 작성일:
- 작성자:

## 질문

<!-- 이 실험으로 답하려는 질문을 한 문장으로 적는다. -->

## 가설

<!-- 기준선, 변경점, 예상 방향과 성공 기준을 수치로 명시한다. -->

## 비교 대상

| 항목 | 기준선 | 실험군 |
| --- | --- | --- |
| Git commit / tag |  |  |
| 브랜치 |  |  |
| 변경점 | 없음 |  |

## 실행 환경

| 항목 | 값 |
| --- | --- |
| 실행 시각 / timezone |  |
| 머신 / OS |  |
| CPU 모델 / 코어 / 할당 제한 |  |
| 메모리 / 컨테이너 할당 제한 |  |
| JVM / Spring Boot |  |
| k6 버전과 부하 발생기 위치 |  |
| MySQL / Redis / Kafka 버전 |  |
| 네트워크 경로 |  |

## 데이터와 fixture

- 테이블별 데이터 크기와 분포:
- 대상 상품 / 시작 재고:
- 캐시 key 수, TTL과 시작 상태(cold/warm):
- outbox 및 consumer offset 시작 상태:
- fixture 초기화 명령 또는 절차:
- 실행 후 정합성 확인 쿼리:

## 트래픽 모델

| 항목 | 값 |
| --- | --- |
| 시나리오 파일 |  |
| 대상 API / method |  |
| 워밍업 시간과 도착률 |  |
| 측정 시간 |  |
| 단계별 목표 RPS |  |
| pre-allocated / max VU |  |
| timeout |  |
| 반복 횟수 |  |

## 정합성과 실패 주입

- 성공으로 인정한 HTTP 상태와 응답 조건:
- 반드시 유지할 불변식(재고 음수 금지, 주문 유실·중복 금지 등):
- 주입한 실패(DB 지연, TTL 동시 만료, broker 중단, consumer 지연 등):
- 실패 주입 시각과 지속 시간:
- 복구 완료 조건과 실제 복구 시간:

## 결과

<!-- 변화율 산식: (실험군 - 기준선) / 기준선 × 100. 수집하지 않은 값은 추정하지 말고 사유를 적는다. -->

| 사용자 관점 지표 | 기준선 | 실험군 | 변화 |
| --- | ---: | ---: | ---: |
| 응답 시간 p50 |  |  |  |
| 응답 시간 p95 |  |  |  |
| 응답 시간 p99 |  |  |  |
| 처리량(성공 RPS) |  |  |  |
| 오류율 |  |  |  |
| dropped iterations |  |  |  |

| 백엔드 지표 | 기준선 | 실험군 | 변화 | 수집 근거 |
| --- | ---: | ---: | ---: | --- |
| 애플리케이션 CPU / memory |  |  |  |  |
| JVM heap / GC pause |  |  |  |  |
| DB QPS / active connections |  |  |  |  |
| 대상 SQL 실행 횟수 / 시간 |  |  |  |  |
| cache hit / miss / loader 호출 |  |  |  |  |
| Redis QPS / latency |  |  |  |  |
| Kafka produce latency / error |  |  |  |  |
| consumer lag / 처리량 |  |  |  |  |
| outbox pending / oldest age |  |  |  |  |
| 대기열 허용 / 대기 / 제한 |  |  |  |  |

## 해석

- 가설 채택 / 기각:
- 사용자 지표와 백엔드 지표를 함께 봤을 때의 원인:
- 성능과 정합성 사이의 trade-off:
- 다음 실험:

## 한계

<!-- 로컬 단일 노드, 짧은 지속 시간, 작은 데이터셋, 관측 누락 등 일반화의 한계를 적는다. -->

## 원본 결과

- 기준선 `summary.json`:
- 기준선 `summary.txt`:
- 기준선 `environment.txt`:
- 실험군 `summary.json`:
- 실험군 `summary.txt`:
- 실험군 `environment.txt`:
- backend dashboard / query log / trace:
