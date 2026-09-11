# Challenge Day 3/120 — 2026-09-11

C003 / B003. 사용자 명시 요청으로 21시 전에 발행. 제출·평가·학습 완료는 아직 아님.
어제: C002 기본8/8 후 O(n) 해답 학습, B002 수정 후 H2 기능11/11+인프라1/1.
독립 발상과 이벤트 보호 이유의 자료 없는 설명은 미확인이다.

오늘 핵심: **기존 예약의 취소와 새 예약이 겹쳐도 응답과 최종 좌석 수가 맞게 만든다.**

## B003 — 먼저 이 상황 하나

정원1, 기존 사용자100의 예약10이 있고 잔여0이다.
새 사용자200이 예약하는 순간 기존 사용자가 취소한다. 두 호출은 별도 트랜잭션이다.

| 실행 관계 | 취소 응답 | 예약 응답 | 최종 기존 예약 / 새 예약 / 잔여 |
| --- | --- | --- | --- |
| 취소 커밋 후 예약 시작 | CANCELLED | RESERVED | 0 / 1 / 0 |
| 예약 매진 커밋 후 취소 시작 | CANCELLED | SOLD_OUT | 0 / 0 / 1 |
| 호출 구간이 겹침 | CANCELLED | 위 두 응답 허용 | 응답에 대응하는 위 상태 |
| 취소가 flush 후 실패·롤백, 예약과 겹침 | 예외 | SOLD_OUT | 1 / 0 / 0 |

진행 중인 취소가 있다는 사실만으로 예약 성공을 요구하지 않는다.
완료 후 예약수+잔여=1이며 각각 0~1이어야 한다. 예외나 NOT_FOUND로 경합을 회피하면 안 된다.
취소 대상이 없거나 다른 이벤트의 예약이면 NOT_FOUND·무변경. 이벤트가 없어도 NOT_FOUND.
취소 재요청은 좌석을 다시 반환하지 않는다. 빈자리에서 새 예약이 중간 실패하면 예약0·잔여1로 원복한다.

[MixedReservationService.java](src/main/java/challenge/MixedReservationService.java)의 두 TODO와 필요한 Repository를 구현한다.
엔티티·생성자 주입·기본 @Transactional·flush/관찰 장치는 제공했다. 평가 대상 로직은 비어 있다.
업무 로직을 테스트의 probe에 넣지 않는다. JVM 내부 잠금만으로 해결하지 않는다.
[공개 테스트](src/test/java/challenge/MixedReservationServiceTest.java)는 별도 Spring 컨텍스트 두 개가 같은 H2 DB를 사용한다.
테스트 전체를 트랜잭션으로 감싸지 않는다. CountDownLatch로 진입/flush 이후를 동기화하며 sleep에 의존하지 않는다.
진입 동기화만으로 내부 SQL 순서가 고정됐다고 주장하지 않는다.

[ORDER.md](ORDER.md)에 실행 순서를 설명하고 기본 모드에서는 공개 테스트 파일 마지막
`userReproducesOneSchedule`을 구현한다. 기존 helper로 별도 트랜잭션·정지·해제를 재사용할 수 있다.
응답만 보지 말고 사용자별 예약과 잔여를 함께 확인한다. 교착·낙관락·격리수준 비교 실험은 추가하지 않는다.


## B003 — 구현할 기능의 정확한 계약

DB의 `event(event_id, remaining)`은 잔여 좌석을, `reservation(reservation_id, event_id, user_id)`은
현재 유효한 예약을 보관한다. 취소는 예약 행 삭제로 표현한다. 정원은 이벤트마다1로 고정하며 별도 capacity 필드는 없다.
기본 fixture: event(1,0), reservation(10,1,100). 신규 사용자는200, ID는 모두 양수다.

| 호출 | 조건 | 반환값 | 커밋 후 변경 |
| --- | --- | --- | --- |
| reserve(eventId,userId) | 이벤트 없음 | NOT_FOUND | 없음 |
| reserve(eventId,userId) | 좌석 없음 | SOLD_OUT | 없음 |
| reserve(eventId,userId) | 좌석 있음 | RESERVED | 새 예약1건 생성, 잔여1 감소 |
| cancel(eventId,reservationId) | 이벤트/예약 없음 또는 다른 이벤트 소속 | NOT_FOUND | 없음 |
| cancel(eventId,reservationId) | 해당 이벤트의 유효한 예약 | CANCELLED | 그 예약 삭제, 잔여1 증가 |

이는 결과 계약 표이며 DB 조회·검사 순서를 지정한 표가 아니다.
예약 ID는 DB가 생성한다. reserve의 반환값에 ID를 추가할 필요 없다. 취소 입력의 두 번째 값은 userId가 아닌 reservationId다.
사용자200의 기존 예약이나 동일 사용자의 중복 예약 요청은 이번 입력에 없다. 취소 재요청은 검사한다.

**실패 계약:** 테스트는 서비스의 업무 처리와 flush가 끝나고 커밋되기 전에 RuntimeException을 주입한다.
예외를 성공 응답으로 바꾸지 말고 호출자에게 전파한다. 삭제/생성만 남거나 좌석 변경만 남으면 실패다.
취소 실패 시 예약10은 남고 잔여0, 빈자리에서 예약 실패 시 새 예약은 없고 잔여1이다.
경합 중 취소가 실패한 경우 새 사용자가 그 미확정 좌석을 성공적으로 가져가면 안 된다.

**직접 작성할 것:** `reserveWithinTransaction`, `cancelWithinTransaction`, 필요한 Repository 선언.
기본 모드는 `userReproducesOneSchedule`의 TODO와 ORDER.md도 작성한다.
컨트롤러/HTTP API, 새 DB 설정, 별도 애플리케이션은 만들 필요 없다.
public 진입점의 @Transactional, EntityManager.flush, TransactionProbe는 제공 인프라다.
probe는 실행 관찰 장치이며 업무 상태를 저장하거나 문제 해결용 잠금으로 사용하지 않는다.

**진행 순서:** 먼저 순차 예약/취소 → 실패 원복 → 두 요청 겹침을 확인한다.
공개 테스트가 내부 구현 방법을 지정하지는 않는다. 내 실행 순서 설명에는
“요청 시작 / 무엇을 읽었는지 / 무엇을 바꿨는지 / 커밋 또는 롤백”을 구별해 적는다.

## 이번에 공부할 것

- **상태의 불변식:** 이벤트별 `유효 예약 수 + remaining = 1`. 개별 메서드 반환과 전체 DB 상태를 함께 판단한다.
- **원자성과 동시 실행의 차이:** 한 요청의 여러 변경을 함께 원복하는 문제와, 두 요청이 같은 상태를 보고 판단하는 문제를 구별한다.
- **트랜잭션 경계와 예외:** Spring 프록시 호출이 끝날 때 커밋/롤백이 어떻게 결정되는지 살펴본다. 기본 RuntimeException 롤백 규칙은 [Spring 문서](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)를 참고한다.
- **flush와 commit:** JPA 변경을 DB에 동기화하는 시점과 트랜잭션 완료를 구별한다. [EntityManager 문서](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/entitymanager)의 flush 부분만 참고해도 된다.
- **재현 가능한 관찰:** CountDownLatch에서 무엇을 기다리고 언제 풀어주는지 기존 테스트의 Hold를 읽어본다. 스레드 시작 순서와 실제 DB 관찰 순서가 같은지 구별한다.

이 개념들을 모두 읽고 나서 시작할 필요는 없다. 해당 테스트에서 막힌 개념만 찾아보고 구현으로 돌아온다.
락 선택·보호 범위는 사용자 판단 대상으로 남긴다. 격리수준 전체 비교나 교착 해결까지 확장하지 않는다.

## C003 — 새 문제: 괄호 문자열 검증

[BracketValidator.java](src/main/java/challenge/BracketValidator.java)의 `boolean isValid(String text)`를 구현한다.
입력은 ()[]{}만 포함하며 길이0~100,000이다. 종류와 중첩 순서가 모두 올바르고 남는 괄호가 없으면 true.
빈 문자열도 true. `([]){}` → true, `([)]` → false, `((` → false.
정확한 풀이부터 구현하고 이후 O(n)을 목표로 한다. 선택한 자료구조의 이유와 복잡도를 설명한다.
구간 합 복습은 철회했으며 원본은 withdrawn에 보존했다. 추가 숙제가 아니다.

## 실행

기존 IntelliJ `study/challenge-120` 프로젝트에서 Gradle 새로고침. JDK21과 기존 Wrapper를 재사용한다.
실행 구성은 `Day 003 - Backend`, `Day 003 - Coding`, `Day 003 - All` 및 각 `Minimum`이다.
별도 프로젝트·worktree를 만들지 않는다.

```sh
cd /Users/justin/IdeaProjects/my-project/study/challenge-120
./gradlew :day-003:test --tests challenge.MixedReservationServiceTest --rerun-tasks
./gradlew :day-003:test --tests challenge.BracketValidatorTest --rerun-tasks
# Minimum은 원하는 명령 뒤에 -Pminimum 추가
```

## 시간·Pass·제출

기본90분(Coding30 / Backend45 / 통합복습15), Minimum45분(15 / 20 / 10) 중 택한다.
Minimum은 순차·롤백 구현과 ORDER 설명까지이며 concurrency 태그4개(사용자 테스트 포함)와 Coding 큰 입력1개는 제외한다.
준비·설치·실행 대기는 풀이 시간에서 제외하며 실제 구현 시작부터 잰다.
통합복습은 기존 별도 복습 대화에서 진행하고 중복 문항을 추가하지 않는다.
미완성이라도 시간 안에 시도한 근거·막힌 점을 제출하면 일일 마감 가능하다.

- Coding: 괄호 종류·중첩·미완료 입력의 정확성, 본인 풀이의 복잡도와 자작 반례.
- Backend: 선택한 범위에서 응답과 최종 DB 상태가 계약에 맞는다.
- 실패: 중간 예외 후 해당 요청의 변경이 전부 원복된다.
- 설명: 관찰한 실행 순서와 상태 보호 범위를 연결한다. 기본은 직접 재현한 순서도 제출한다.

제출은 모드·실제 시간·첫 접근·코드/테스트·ORDER 설명·막힌 점·도움 범위를 간단히 적어도 된다.
제공 테스트의 경계를 독립 발견으로 세지 않고, 틀 사용은 독립성 감점 사유가 아니다.

## 환경 검증

아래는 준비 검사이며 사용자 수행 결과가 아니다. H2만 사용한다.
MySQL·성능·모든 경합 스케줄은 미검증이다.

최초 발행 당시 검증(구간 합 문제 교체 전): 기존 Wrapper/JDK21에서 main/test 컴파일 및 테스트 실행 확인.
전체18개: 인프라2개 통과, 기능·사용자 재현 TODO16개는 의도적 실패.
Minimum14개: 인프라2개 통과, TODO12개 의도적 실패, concurrency4개 제외 확인.
인프라 검사는 별도 연결의 미커밋 변경 비노출과 주입 예외 후 롤백까지 확인했다.
업무 롤백/경합 테스트의 통과는 사용자 구현 후 검증해야 한다.
실행 구성 XML5개와 README/ORDER 링크 확인. 환경 오류 없음. H2만 실행, MySQL 미검증.

교체 후 검증: main/test 컴파일 성공. 새 Coding 공개 테스트9개 발견·실행, 모두 미구현 TODO 예외로 의도적 실패. 실행 구성 XML5개·상대 링크·이전 Coding 실행 제외 확인. Backend는 설명만 변경하여 기존 검증을 재사용했고 재실행하지 않았다.
