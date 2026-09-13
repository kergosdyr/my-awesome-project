# Challenge Day 5/120 — 2026-09-13

C005 / B005 최초 발행. 기본 **Coding 30분 + Backend 45분 + 통합 복습 15분 = 90분**.
Minimum은 같은 과제의 **15 + 20 + 10 = 45분**이며 택일이다. 실제 구현 시작부터 시간을 재고 AI 환경 준비·대기는 제외한다.

어제: C004 구간 합치기 정답 학습, B004 고정 총3회 Spring Retry 구현·MySQL17/17 및 경합/jitter 실험 완료.
독립 설명·실제 시간은 미확인. 기존 약점은 [장부](../../ledger.md)에 유지하며 오늘 반복 출제를 강제하지 않는다.

오늘 핵심: **응답을 못 받은 결제를 나중에 다시 확인해도 승인과 로컬 상태가 중복되거나 뒤로 돌아가지 않게 한다.**

## Coding — C005 다음으로 더 높은 측정값

[NextHigher.java](src/main/java/challenge/coding/NextHigher.java)의 `findIndices(int[] values)`를 구현한다.
각 위치에서 오른쪽으로 처음 만나는 **엄격히 큰 값의 인덱스**를 반환한다. 없으면 -1.
입력은 null 아님, 길이 0..200,000, int 전체 범위. 입력 보존·새 반환 배열.
인덱스는 **0부터 시작**하며 값 자체가 아니라 위치를 반환한다.

**예제 1**

```text
입력: values = [2,1,2,4,3]
출력: [3,2,3,-1,-1]
```

위치 0의 2보다 큰 첫 값은 위치 3의 4다. 위치 2의 2는 같은 값이므로 제외한다.
위치 1의 1은 위치 2의 2를, 위치 2의 2는 위치 3의 4를 만난다.
위치 3과 4에는 오른쪽에 더 큰 값이 없다.

**예제 2**

```text
입력: values = [2,3,1,9]
출력: [1,3,3,-1]
```

위치 0의 답은 가장 큰 값 9의 위치 3이 아니라 처음 만나는 3의 위치 1이다.
위치 1과 2의 답은 모두 9의 위치 3이다. 마지막 위치의 답은 -1이다.

**예제 3**

```text
입력: values = [4,4,4]
출력: [-1,-1,-1]
```

같은 값은 엄격히 큰 값이 아니므로 모든 위치에 답이 없다. 빈 입력 `[]`의 출력은 `[]`다.

첫 시도는 `[3,1,3,2,5]`의 답을 손으로 적고 기본 구현부터 만든다. 기본 정확성과 O(n) 목표 달성은 따로 평가한다.
Minimum은 작은 입력 기본 구현까지. 공개 테스트10개 중7개를 실행한다. 최적화는 기본 풀이 이후 같은 시간 안에서 시도한다.

## Backend — B005 승인됐지만 응답을 잃은 결제

2026-09-14 현재 **작성 중·일단 보류**다. 정상 예약 조회와 PG 승인 호출까지 사용자가 작성했다.
아래는 재개할 때 참조할 계약이며, 오늘 이 과제를 끝내거나 같은 설명을 다시 제출할 의무는 없다.

### 무엇을 확인하는 실습인가

예약 1번에 1,000원을 결제했는데 화면에 결과가 오지 않았다. 사용자가 다시 버튼을 누르거나
늦은 승인 알림이 도착해도 두 번 승인하지 않고 결과를 확인하는 서비스 로직을 다룬다.
가짜 PG는 이미 승인했지만 우리 DB는 아직 모르는 상황이 핵심이다.

| 구분 | 의미 |
| --- | --- |
| PG `PROCESSING` | 접수됐지만 PG가 아직 승인하지 않음 |
| PG `APPROVED` | PG에서 승인 완료 |
| `ResponseLostException` | 응답을 받지 못한 통신 사건. 이 fake에서는 승인 후 발생 |
| 로컬 `PENDING` | 우리 DB에서 승인을 아직 확정하지 못함. 타임아웃 자체를 뜻하지 않음 |
| 로컬 `PAID` | 승인 결과를 확인하고 우리 DB에도 완료로 기록 |

PG가 PROCESSING을 응답한 경우는 처리 중임을 **알고** 기다린다.
응답 유실인 경우는 PG의 현재 상태를 **모르고** 기다린다. 이 작은 실습은 둘 다 로컬 PENDING으로 표현한다.
이는 실제 PG 공통 상태 정의가 아니라 이 실습의 단순화된 계약이다.

```text
첫 결제 요청  → PG 승인 완료 → 응답 유실 → 로컬 PENDING, 사용자에게도 PENDING
나중 재요청   → 같은 결제 결과 확인     → 로컬 PAID, 승인 ID 반환
늦은 승인 알림 → 이미 PAID라면 결과 유지 → 추가 승인 없음
```

첫 호출 안에서 즉시 재시도/조회하지 않는 것은 두 관찰 시점을 나누기 위한 실습 제약이다.
실서비스에서는 지연 예산·PG 계약에 따라 달리 정할 수 있으며 이 조건 자체를 설계 정답으로 평가하지 않는다.

### 구현 범위와 한계

`pay`와 `onNotification`, 필요한 Repository 조회를 구현하는 서비스 단위 실습이다.
API/실제 PG 연동/앱 재시작 후 복구까지 포함한 결제 시스템을 만들었다고 평가하지 않는다.
상태와 테스트를 미리 정해둔 탓에 설계 선택이 적다는 과제 구성의 한계가 있다.
Redis·Kafka 추가나 과제 규모 확대만으로 이 한계를 해결하지 않는다.
후속 실습은 업무 상황·관찰 가능한 결과·사용자가 정할 설계 범위를 먼저 명확히 하고 기존 시간 안에서 구성한다.

### 현재 동작 계약

Day 5만으로 실행되는 독립 Spring/JPA 실습이다. 제공된 예약 ID fixture를 결제 대상으로 사용하며 다른 날짜 프로젝트에 의존하지 않는다.
새 [PaymentService.java](src/main/java/challenge/payment/PaymentService.java)의 `pay`, `onNotification`과
[PaymentRepository.java](src/main/java/challenge/payment/PaymentRepository.java)의 필요한 조회만 구현한다.
Entity·JpaRepository·생성자 주입·기본 @Transactional·가짜 외부 시스템·DB fixture는 제공한다.

학습 목표는 외부 시스템과 로컬 DB의 결과를 구분하고 재요청·알림의 업무 계약을 지키는 것이다.
필요한 개념은 JPA 저장·조회, 트랜잭션 롤백 범위, 외부 API의 요청 식별 계약과 상태 전이다.
락 비교나 요청별 재시도 횟수 구현은 요구하지 않는다.

| 입력/상황 | 관찰 가능한 계약 |
| --- | --- |
| `pay(예약ID, 양수 금액)` 정상 승인 | PAID + 승인ID 반환, DB에도 같은 결과 |
| 없는 예약 | NOT_FOUND + null, 외부 호출·결제 행 생성 없음 |
| 외부 승인 후 응답 유실 | 그 호출은 PENDING + null, DB에 PENDING. 즉시 조회/재시도하지 않음 |
| 동일 예약·금액의 순차 재요청 | 외부 결과가 승인됐으면 같은 승인ID로 PAID. 실제 승인1건·DB 결제1행 |
| 외부가 아직 PROCESSING | PENDING + null 유지 |
| 같은 예약의 다른 금액 | CONFLICT + null, 외부 호출·DB 변경 없음 |
| 알려진 key·예약·금액의 승인 알림 | true, DB PAID. 새 승인 호출 없음 |
| 같은 승인 중복·PAID 후 늦은 PROCESSING 알림 | true, PAID와 승인ID 보존 |
| 모르는 key 또는 예약/금액 불일치 알림 | false, 변경 없음 |

예: 첫 pay=PENDING(결제사는 이미 승인), 나중 pay=PAID, 중복 알림 후에도 PAID.
PENDING/PAID만 DB 업무 상태로 저장한다. NOT_FOUND/CONFLICT는 응답 전용이다.
모든 호출은 순차이며 예약은 이미 확정되어 오늘 실행 중 취소/만료되지 않는다. 결제는 예약 fixture를 변경하지 않는다. 재고·예약 생성·취소 구현은 포함하지 않는다.
양수 금액·유효 문자열·형식 검증·사용자 인증·알림 서명 검증은 완료된 입력을 준다.
같은 결제의 APPROVED는 항상 같은 승인ID이고 PROCESSING의 승인ID는 null이다.
동시 요청·환불·거절·프로세스 사망·자동 복구 배치는 범위 밖이다.

가짜 결제사는 [PaymentGateway](src/main/java/challenge/payment/PaymentGateway.java)의 계약대로 동작한다.
같은 key/예약/금액 호출은 같은 결제이고 다른 payload는 예외다. 조회는 새 승인을 만들지 않는다.
[FakePaymentGateway](src/test/java/challenge/payment/FakePaymentGateway.java)는 정상 승인, 승인 후 응답 유실,
PROCESSING 후 수동 완료를 제공한다. 테스트가 알림을 직접 전달하므로 서버나 sleep 없이 장애 순서를 재현한다.

Minimum은 pay의 정상/유실/동일 재요청/없는 예약4개 계약까지만 구현한다. 알림·금액충돌 등 나머지는 미검증으로 남긴다.

## 실행

기존 IntelliJ 프로젝트 `/Users/justin/IdeaProjects/my-project/study/challenge-120`를 사용한다.
처음 여는 경우 이 폴더를 Gradle 프로젝트로 연다. 자동 탐색 settings와 루트 Wrapper를 재사용한다.
실행 구성: `Day 005 - Coding`, `Backend`, `All`, `Coding Minimum`, `Backend Minimum`, `Backend MySQL`(모두 `Day 005 -` 접두사).
Gradle 모듈 목록은 새로고침 후 day-005가 표시된다.

터미널 작업 경로는 아래 폴더다.

```sh
cd /Users/justin/IdeaProjects/my-project/study/challenge-120
./gradlew :day-005:test --tests 'challenge.coding.*'
./gradlew :day-005:test --tests 'challenge.payment.*'
./gradlew :day-005:test
# Minimum (기본과 택일)
./gradlew :day-005:test -Pminimum
# 선택: Docker 실행 상태에서 전용 일회용 MySQL 8.4 컨테이너
./gradlew :day-005:test -Pmysql --tests 'challenge.payment.*'
# 제공 환경만 검사: 업무 TODO와 독립
./gradlew :day-005:test --tests '*PaymentInfrastructureTest'
```

JDK21과 기존 의존성을 사용한다. 기본은 H2 MySQL 모드의 격리된 메모리 DB, MySQL도 테스트 전용 DB를 자동 생성한다.
Hibernate가 제공 Entity로 스키마를 생성하고 테스트가 예약 ID fixture를 직접 저장한다.
사용자 DB/볼륨을 사용하지 않는다. H2는 MySQL 동시성·격리 검증을 대체하지 않는다.
테스트 자체는 일괄 트랜잭션으로 감싸지 않아 서비스 반환 이후 커밋된 상태를 확인한다.

## Pass와 제출

1. Coding의 값·인덱스/동등값/끝 조건을 지킨다. 복잡도 목표 달성은 별도로 설명한다.
2. 결제 응답 유실 후 재요청에서도 실제 승인이 중복되지 않고 DB·응답이 일치한다.
3. 기본 선택 시 중복·늦은·불일치 알림을 계약대로 처리하며 예약 fixture가 보존된다.
4. 외부 승인 후 DB 커밋 자체가 실패하면 오늘 보장의 무엇이 깨지는지 짧게 설명한다(구현 확장 숙제 아님).

초기 접근, 실제 시간, 코드/테스트 결과, 스스로 고려한 반례, 자료 없는 짧은 이유, 막힌 점·AI/문서 도움 범위를 제출한다.
짧게 대화로 답해도 된다. 공개 테스트/틀은 독립성 감점 제외이며 공개 경계는 독립 발견으로 세지 않는다.
통합 복습은 별도 작업의 기존 슬롯을 사용하고 여기서 추가 질문을 발행하지 않는다. Kotlin Redis는 오늘 범위에 포함하지 않는다.

## 준비 검증

2026-09-13 사용자 요청으로 예전 실험 프로젝트 의존성을 제거했다. Coding의 사용자 구현은 보존했다.
결제의 업무 TODO는 유지하고 독립 Spring 설정·예약 ID Entity/Repository·테스트 fixture를 제공한다.
정리 후 전체 실습 컴파일, C00510개·H2 인프라4개·MySQL 인프라4개가 통과했다.
결제 업무9개는 H2/MySQL 모두 TODO 예외로 의도적 실패다. 자세한 결과는 [9월13일 세션](../../../sessions/2026-09-13.md)에 기록했다.
과거 예약/취소 회귀 검사는 이 실습의 현재 실행 대상이 아니다.
