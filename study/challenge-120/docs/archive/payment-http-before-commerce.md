> 커머스 전환 전 Day6 안내 원본. 현재 과제는 [payment-http](../exercises/payment-http.md)를 따른다.

# Challenge Day 6/120 · 2026-09-14 · 결제 HTTP 흐름으로 정정

1. **오늘 해결할 문제**: C006 첫 유일 문자 찾기와 B006 결제 요청→PG 승인 알림→저장된 결과 조회를 실제 HTTP로 실행한다.
2. **왜 이 문제가 중요한지**: 서비스 테스트만 통과해도 외부 요청·알림이 연결되지 않으면 사용자가 결제 결과를 확인할 수 없고, 반복 알림이 새 결제 행을 만들면 상태가 불일치한다.
3. **오늘 배울 핵심 개념**: 요청/알림의 진입점, 조합 가능한 조회·생성 부품(Reader/Saver), 결제 객체가 소유하는 상태 전이.
4. **완료 기준**: 선택한 공개 테스트 실행, HTTP 요청부터 DB 변경까지 추적, 같은 승인 알림의 반복 처리와 Payment의 상태·승인ID 규칙 설명. C006은 정확성과 복잡도를 별도 확인한다.

## Day5에서 이어가기

Order 과제는 사용자 정정으로 철회했다. C006은 유지하며 B006은 기존 결제 코드에 HTTP 흐름을 연결한다. Day5 핵심6개 통과는 당시 좁은 계약의 통과이며 결제 시스템 전체 완성을 뜻하지 않는다. 알림은 어제 필수가 아니었고, **오늘부터 명시적으로 추가한 새 범위**다. Day5를 다시 마감하라고 요구하지 않는다.

기존 PaymentService/Payment 사용자 구현은 [결제 패키지](../../legacy/main/java/challenge/payment)에 계속 있다. HTTP와 결제 코드가 단일 Gradle 프로젝트에 있어 수정은 한 곳에서 한다. 종료한 프로젝트는 재사용하지 않는다. Day1~30의 트랜잭션·Java 객체 책임을 실제 요청에 적용하는 단계이며 이후 메시징/장애 복구의 기초가 된다. Redis/Kafka나 별도 대형 시스템은 추가하지 않는다.

## C006 — 처음 한 번만 등장한 문자

[FirstUnique.java](../../src/main/java/challenge/coding/FirstUnique.java)의 `find(String text)`를 구현한다.

- 입력: null이 아닌 영문 소문자 `a`~`z`, 길이 0~100,000.
- 출력: 문자열 **전체에서 정확히 한 번** 등장한 문자 중 가장 앞선 0 기반 인덱스. 없으면 `-1`.
- 입력을 변경하지 않는다. 대문자·한글·null 처리는 오늘 계약 밖이다.

| 입력 | 출력 | 설명 |
| --- | --- | --- |
| `"leetcode"` | `0` | `l`은 한 번 등장하며 가장 앞이다. |
| `"loveleetcode"` | `2` | `l`, `o`는 반복되므로 첫 유일 문자는 `v`다. |
| `"aabb"` | `-1` | 모든 문자가 반복된다. |
| `""` | `-1` | 후보가 없다. |

첫 시도: `abac`를 종이에 적고 각 후보를 선택하거나 버린 근거를 말한다. 작은 입력을 손으로 해결한 뒤 느려도 정확한 구현을 작성한다. 공개 기본8개 → 반복 작업 확인 → O(n) 최적화·상한 입력1개 순서다. O(n) 달성 여부는 코드와 설명으로 평가하며, 상한 입력 테스트 통과만으로 효율성을 증명하지 않는다. 시간 안에 기본 풀이만 완성해도 정확성과 최적화를 분리 평가한다. 정답·풀이 패턴은 먼저 제공하지 않는다.

## B006 — 운영자가 직접 실행할 수 있는 결제 흐름

클라이언트가 예약1의 1,000원 결제를 요청한다. PG가 아직 승인 전이면 우리 서버는 PENDING을 반환한다. 이후 PG가 승인을 완료하고 HTTP 알림을 보내면, 사용자가 결과 조회로 같은 결제의 PAID와 승인 ID를 확인할 수 있어야 한다. PG는 전송 결과를 확신하지 못해 같은 알림을 다시 보낼 수 있다.

원인 질문: 결제 요청의 응답이 끝난 뒤 PG 승인이 완료되면, 우리 DB는 어떤 사건을 통해 그 사실을 알게 될까? PG 원장을 바꾸는 것만으로 우리 결제 행도 바뀔까?

### 오늘 고정 계약

- 결제 클라이언트: 예약 ID와 양수 원화 금액을 보내는 앱/사용자. fixture 예약1·2·3을 제공한다.
- PG: `PaymentGateway` 경계 뒤의 가짜 결제사. PROCESSING은 PG 승인 전, APPROVED는 승인 완료다. 실제 돈·외부 네트워크 결제는 없다.
- 우리 Payment: PENDING은 로컬 승인 미확정, PAID는 로컬 승인 확인이다. PG 상태와 주체가 다르다.
- 입력은 동일 예약·동일 key·동일 금액이며 순차 요청이다. 알려진 결제 알림은 APPROVED이고 유효한 동일 승인 ID를 가진다. 현재 key는 기존 사용자 정책인 예약 ID 문자열이다.
- `POST /payments` 정상 PAID는200, PENDING은202. 없는 예약은404. 양수가 아닌 ID/금액 등 HTTP 입력 오류는400.
- `GET /payments/{reservationId}`는 로컬 기록만 조회한다. PAID200/PENDING202, 결제 행이 없으면404. PG 승인/상태 동기화를 일으키지 않는다.
- `POST /payments/notifications`: 알려진 결제의 유효한 승인 알림은200 `{ "accepted": true }`. 기존 결제 행 ID를 유지하면서 승인 상태·승인 ID를 일치시킨다.
- 같은 승인 알림이 반복되거나 이미 동기 승인된 결제에 도착해도200/true, 결제1행·같은 승인 ID·PG 승인1건을 유지한다.
- 로컬 결제 행이 없는 알림은200/false, 새 결제를 만들거나 PG 승인을 호출하지 않는다. 실습의 명시적 업무 선택이며 모든 PG의 표준 정책은 아니다.
- 금액 변경/위조·불일치 알림/PROCESSING 알림/서명 검증/동시 요청/취소·환불/자동 재전송/전체 PENDING 복구는 오늘 범위 밖이다. PG 승인 후 **알림 없이 pay만 다시 호출하는 경우**도 오늘 새 완료 기준이 아니다.
- 실험 PG와 DB는 프로세스 종료 시 초기화된다. 실제 별도 PG 프로세스·네트워크 장애·재기동 영속성 시험으로 주장하지 않는다. HTTP는 실제 socket 요청이며 MockMvc나 서비스 직접 호출로 대체하지 않는다.

### 실제 진입점과 조합 구조

```text
사용자 HTTP -> PaymentController -> PaymentService.pay
PG 알림 HTTP -> PaymentController -> PaymentService.onNotification
조회 HTTP   -> PaymentController -> PaymentQueryService.find
                                       |
                    PaymentReader / ReservationReader / PaymentSaver
                                       |
                          Payment / JPA Repository

PaymentService -> PaymentGateway <- LabPaymentGateway (제공 가짜 PG)
```

Reader/Saver는 여기서 말하는 **implementation 계층의 조합 부품**이다. 각 클래스에 interface/Impl 쌍을 일괄 추가하지 않는다. PaymentReader는 결제 요청·결과 조회에서 이미 재사용하며 알림에서도 조합할 수 있다. PaymentSaver는 새 결제 생성·영속화를 맡는다. 기존 결제의 상태 변경은 Payment의 업무 동작으로 표현한다. 트랜잭션은 Service가 소유하고 Reader/Saver는 같은 트랜잭션에 참여한다.

현재 flat `challenge.payment`와 JpaRepository 계약은 기존 학습 코드 호환을 위해 유지한다. Controller에는 저장소를 주입하지 않고 HTTP DTO를 반환하며, OSIV는 꺼 뒀다. 연관 엔티티 로딩·전체 패키지 재배치·Repository interface/Impl 전환은 오늘 필수가 아니다. 로컬 PG 조작 Controller만 제공 실험 장치의 facade를 사용한다.

**제공한 부분**: Controller/JSON/검증/HTTP 상태 매핑, 결과 조회 Service, Reader·Saver 추출, Spring/JPA 보일러플레이트, 로컬 서버, 가짜 PG 조작 API, fixture, HTTP 테스트와 요청 파일, Spotless. 사용자 `pay`와 `onNotification`의 판단·저장 동작은 보존하고 호출 대상을 Reader/Saver로 추출했다. 기존 중첩 흐름 자체를 새 정답으로 대체하지 않았다.

**직접 설계·구현할 부분**:

- [PaymentService.onNotification](../../legacy/main/java/challenge/payment/PaymentService.java): 현재 사용자 작성본을 보존했다. TODO 주석의 알림 계약을 구현한다.
- [Payment](../../legacy/main/java/challenge/payment/Payment.java): 상태와 승인 ID를 함께 다루는 업무 동작을 설계하고 알림 흐름에서 사용한다. 단순 setter 조합을 어디에서 제한할지 설명한다.
- 현재 `PaymentResult.Status`가 엔티티에도 쓰이는 결합, PAID 생성·승인 ID의 유효성, setter 노출은 설계 검토 지점이다. 별도 상태 enum 분리는 선택 가능하며 새로운 필수 숙제가 아니다. 타입/메서드 이름이나 호출 횟수는 테스트에서 강요하지 않는다.

업무 정답은 미리 구현하지 않았다. 서비스에 테스트용 callback/flush/latch/수동 트랜잭션도 넣지 않는다. 오늘 설계 판단과 구현 완료를 사용자 대신 기록하지 않는다.

### 구현 전 질문3개와 비교1개

1. 알림이 도착했을 때 어떤 기존 결제를 읽어야 하며, 로컬 결제가 없다면 어떤 결과를 돌려줄까?
2. 상태와 승인 ID가 서로 모순되지 않도록 하는 책임을 Service와 Payment 사이에 어떻게 나눌까?
3. 같은 승인 알림이 두 번 도착했을 때 보존되어야 하는 값은 무엇이며 Reader·Saver 중 어떤 부품을 조합할까?

비교할 트레이드오프는 **Service에 모든 판단·영속화 코드를 두는 방식과 Reader/Saver·Payment에 책임을 나누는 방식**이다. 재사용·규칙 집중의 이점과 클래스 간 이동·과도한 래핑 비용을 자신의 코드로 설명한다.

## 실행 — Controller를 통해 직접 확인

```sh
cd /Users/justin/IdeaProjects/my-project/study/challenge-120
./gradlew run
```

IntelliJ `payment-http - Server` 또는 위 명령으로 `127.0.0.1:18086`에서 시작한다. 서버가 뜬 뒤 [requests.http](../../requests/legacy-payment.http)를 위에서 아래로 실행하거나 별도 터미널에서 다음 제공 클라이언트를 실행한다.

```sh
python3 tools/payment-flow.py
```

이 클라이언트는 가짜 PG에서 실제 승인 receipt를 받아 HTTP 알림을 두 번 전송하고, 중간/최종 로컬 상태를 출력한다. PG 완료 API는 원장만 바꾸고, 이 클라이언트가 PG의 알림 전송을 대신한다. 실제 PG HTTP 호출을 흉내 낸 별도 outbound adapter를 사용자에게 구현시키는 과제는 아니다.

처음부터 재현하려면 서버를 종료하고 다시 시작한다(실습 메모리 데이터 초기화). 기대 흐름은 `PENDING202 → PG APPROVED → 조회 PENDING202 → 알림200/true → 중복 알림200/true → 조회 PAID200`이다. 현재 알림 구현에서는 오류가 관찰되며 아래 새 계약 테스트와 같은 구현 과제다.

```sh
# 자동 포맷: 결제 패키지와 Day6에만 적용, C001~C005 사용자 풀이 제외
./gradlew spotlessApply
./gradlew spotlessCheck
# 기존 Day5 핵심 회귀: 알림 확장은 여전히 기본에서 제외
./gradlew test --tests 'challenge.payment.*'
# 제공 HTTP 환경/기존 정상 결제
./gradlew test --tests 'challenge.lab.PaymentHttpInfrastructureTest'
# 오늘 신규 알림 계약
./gradlew test --tests 'challenge.lab.PaymentNotificationContractTest'
# 코딩 / 오늘 전체 / Minimum (택일)
./gradlew test --tests 'challenge.coding.*'
./gradlew test
./gradlew test -Pminimum
# 선택 MySQL (Docker, 테스트 전용 DB)
./gradlew test -Pmysql --tests 'challenge.lab.*'
# 실행 배포본과 컴파일: TODO 때문에 업무 테스트 통과를 뜻하지 않음
./gradlew installDist testClasses
```

Spotless7.2.1 + google-java-format1.28.0 AOSP(4칸)를 고정했다. 한 줄에 몰린 선언/본문을 펼치고 긴 호출을 자동 줄바꿈한다. [Spotless 공식 플러그인](https://plugins.gradle.org/plugin/com.diffplug.spotless/7.2.1). IDE 포맷터와 규칙이 다르면 위 명령을 기준으로 한다. 루트 `check`는 `spotlessCheck`를 포함하며 기존 CI 필수 검사는 유지한다.

## 시간과 제출

기본90분 = Coding30(수작업5/기본15/최적화·검증10) + Backend45(HTTP 관찰5/설계8/구현22/실행·설명10) + 기존 통합복습15.
Minimum45분 = Coding15(기본8개) + Backend20(관찰3/설계4/구현8/실행·설명5) + 통합복습10. Backend는 승인 알림·중복 알림·없는 결제 알림3개를 확인하고 동기 승인 뒤 알림1개는 `full`로 제외한다. Minimum을 전체 완료로 평가하거나 남은 검사를 밀린 숙제로 강제하지 않는다.

환경 설치/실행 대기는 풀이 시간에서 제외한다. 상태 전용 enum 등 구조 검토는 구현 시간 안에 선택하며 범위를 사후 확장하지 않는다. Phase·Weekly Review·Benchmark와 통합복습 중복 방지 원칙은 유지한다.

제출은 실제 HTTP/테스트 결과, 초기 접근, 사용 시간·받은 도움, Payment의 규칙과 Reader/Saver 책임 설명이면 된다. 구현 전 결과를 추정하거나 점수를 매기지 않는다.

## 준비 검증 · 2026-09-14 정정판

컴파일·installDist·Spotless 검사 통과. H2/MySQL 각각 Day5 회귀11/11, Day6 HTTP 환경5/5 통과. 새 알림4개는 기존 구현 미완성으로 실패한다(기존 행 대상3개 UNIQUE/HTTP500, 없는 결제1개 잘못된 생성·true 반환). Minimum은 HTTP 환경5/5, 알림3개 업무 실패, C0068개 TODO 실패다. 실제 서버·Python HTTP 클라이언트에서도 동일한 미완성 흐름을 확인하고 확인용 서버를 종료했다. 사용자 상태 전이·알림 정답·독립 설명은 미완료이며 새 과제의 구현 대상으로 남겼다.
