# 이전 학습 구현

커머스 전환 직전 예약·결제·HTTP 구현과 공개 테스트를 원문 그대로 보존한다. 활성 앱은 `src/main/java/challenge/commerce`이며 여기 코드를 새 기능의 출발점으로 복사하지 않는다.

| 경로 | 이전 결과물 |
| --- | --- |
| `main/java/challenge/reservation/basic` | 예약 기본 |
| `main/java/challenge/reservation/cancellation` | 취소 |
| `main/java/challenge/reservation/concurrency` | 경합 |
| `main/java/challenge/payment` | 기존 사용자 결제·응답 유실 풀이 |
| `main/java/challenge/lab` | 예약 fixture를 사용한 결제 HTTP |
| `test/java` | 같은 패키지의 공개 검사와 부하 실행기 |
| `main/resources` | 실습별 schema·HTTP 설정 |

루트의 단일 Gradle 프로젝트에서 `legacy`/`legacyTests` 소스 세트로 컴파일한다. 현재 커머스 실행 classpath에는 포함하지 않는다.

```sh
cd study/challenge-120
./gradlew legacyTestsClasses
./gradlew legacyTest
./gradlew legacyTest --tests 'challenge.reservation.basic.*'
./gradlew legacyTest --tests 'challenge.payment.*' -Pmysql
./gradlew legacyTest --tests 'challenge.payment.*' -PpaymentExtensions
./gradlew legacyPaymentServer
python3 tools/legacy-payment-flow.py
```

현재 서버와 과거 HTTP 서버는 같은18086포트이므로 하나씩 실행한다. 기존63개 검사는58개 통과·5개 실패(기존 알림4, 경합1)다. 답안이나 기대값을 바꾸지 않았다. 이전 설정 원문은 `docs/archive/build-configs/*.txt`, 전환 전 Day6 안내는 `docs/archive/payment-http-before-commerce.md`에 있다.
