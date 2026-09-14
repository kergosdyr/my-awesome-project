# Challenge Day 3/120 — 2026-09-11

C003 / B003. 사용자 명시 요청으로 21시 전에 발행.
C003: 사용자 구현 후 피드백을 반영하여 공개 테스트 9/9 통과(길이 100,000 포함).
코드 검토상 시간 O(n), 공간 O(n). 독립 최초 풀이 통과와 구분하며 실제 소요시간은 미기록이다.
B003: 없는 이벤트 예약의 NOT_FOUND 처리는 아직 미완료다.
아래 `build/` 결과·대시보드 링크는 로컬 실행 산출물이며 Git에는 포함하지 않는다.
어제: C002 기본8/8 후 O(n) 해답 학습, B002 수정 후 H2 기능11/11+인프라1/1.
독립 발상과 이벤트 보호 이유의 자료 없는 설명은 미확인이다.

현재 작업 코드는 [ReservationService.java](../../legacy/main/java/challenge/reservation/concurrency/ReservationService.java)와
[CancellationService.java](../../legacy/main/java/challenge/reservation/concurrency/CancellationService.java)다.
Day 1 예약·Day 2 취소의 사용자 구현 본문을 그대로 이어받았으며 새로 작성할 필요 없다.
예약ID/EXISTING 응답, 엔티티 동작, Repository 조회도 보존했다. 양일 원본 파일은 수정하지 않았다.
오늘은 기존 구현에 혼합 경합 테스트를 추가하고 실제로 부족한 요구사항만 수정한다.
이후 같은 백엔드 주제도 이 구현을 이어서 발전시키며 날짜가 바뀌었다고 초기화하지 않는다.

오늘 핵심: **기존 예약의 취소와 새 예약이 겹쳐도 응답과 최종 좌석 수가 맞게 만든다.**

## B003 — 먼저 이 상황 하나

정원1, 기존 사용자100의 예약10이 있고 잔여0이다.
새 사용자200이 예약하는 순간 기존 사용자가 취소한다. 두 호출은 별도 트랜잭션이다.

| 실행 관계 | 취소 응답 | 예약 응답 | 최종 기존 예약 / 새 예약 / 잔여 |
| --- | --- | --- | --- |
| 취소 커밋 후 예약 시작 | CANCELLED | CREATED | 0 / 1 / 0 |
| 예약 매진 커밋 후 취소 시작 | CANCELLED | SOLD_OUT | 0 / 0 / 1 |
| 호출 구간이 겹침 | CANCELLED | 위 두 응답 허용 | 응답에 대응하는 위 상태 |
| 취소가 커밋 전 실패·롤백, 예약과 겹침 | 예외 | SOLD_OUT | 1 / 0 / 0 |

진행 중인 취소가 있다는 사실만으로 예약 성공을 요구하지 않는다.
완료 후 예약수+잔여=1이며 각각 0~1이어야 한다. 예외나 NOT_FOUND로 경합을 회피하면 안 된다.
취소 대상이 없거나 다른 이벤트의 예약이면 NOT_FOUND·무변경. 이벤트가 없어도 NOT_FOUND.
취소 재요청은 좌석을 다시 반환하지 않는다. 빈자리에서 새 예약이 중간 실패하면 예약0·잔여1로 원복한다.

기존 예약/취소 서비스를 유지하며 테스트에서 드러난 필요한 변경만 구현한다.
엔티티·JpaRepository·생성자 주입·기본 @Transactional을 제공했다. 서비스는 일반적인 JPA 업무 코드로 작성한다.
검증을 위한 EntityManager·flush·콜백·동기화 코드를 서비스에 넣지 않는다. JVM 내부 잠금만으로 해결하지 않는다.
[공개 테스트](../../legacy/test/java/challenge/reservation/concurrency/MixedReservationServiceTest.java)는 별도 Spring 컨텍스트 두 개가 같은 H2 DB를 사용한다.
테스트 메서드 전체를 트랜잭션으로 감싸지 않는다. 순차 검사는 서비스 자체 트랜잭션을 사용한다.
경합/장애 검사는 worker별 독립 트랜잭션에 서비스가 참여하도록 테스트 내부에서 제어한다.
테스트의 Repository.flush와 CountDownLatch로 커밋 전 DB 쓰기·중단·예외를 제어하며 sleep에 의존하지 않는다.
이 제어 테스트는 서비스의 기본 REQUIRED 참여를 전제로 한다. 서비스 자체 트랜잭션을 사용하는 동시 호출도 JUnit 반복 검사5회로 별도 확인한다.
Awaitility는 요청 완료/관찰 지점의 조건 대기를 담당하고 실제 동시 실행은 별도 worker가 담당한다. [공식 사용법](https://github.com/awaitility/awaitility/wiki/Usage).
진입 동기화만으로 내부 SQL 순서가 고정됐다고 주장하지 않는다.

[ORDER.md](reservation-implementation-order.md)는 선택적인 설명 메모다. 메시지로 설명해도 되며 파일 작성은 필수가 아니다.
경합 재현·장애 주입 테스트는 AI가 제공하므로 사용자가 테스트 장치를 구현할 필요 없다.
응답만 보지 말고 사용자별 예약과 잔여를 함께 확인한다. 교착·낙관락·격리수준 비교 실험은 추가하지 않는다.


## B003 — 구현할 기능의 정확한 계약

DB의 `event(event_id, remaining)`은 잔여 좌석을, `reservation(reservation_id, event_id, user_id)`은
현재 유효한 예약을 보관한다. 취소는 예약 행 삭제로 표현한다. 정원은 이벤트마다1로 고정하며 별도 capacity 필드는 없다.
기본 fixture: event(1,0), reservation(10,1,100). 신규 사용자는200, ID는 모두 양수다.

| 호출 | 조건 | 반환값 | 커밋 후 변경 |
| --- | --- | --- | --- |
| reserve(eventId,userId) | 이벤트 없음 | NOT_FOUND + null | 없음 |
| reserve(eventId,userId) | 기존 사용자 예약 있음 | EXISTING + 기존 예약ID | 없음 |
| reserve(eventId,userId) | 기존 예약·좌석 없음 | SOLD_OUT + null | 없음 |
| reserve(eventId,userId) | 기존 예약 없고 좌석 있음 | CREATED + 새 예약ID | 새 예약1건 생성, 잔여1 감소 |
| cancel(eventId,reservationId) | 이벤트/예약 없음 또는 다른 이벤트 소속 | NOT_FOUND | 없음 |
| cancel(eventId,reservationId) | 해당 이벤트의 유효한 예약 | CANCELLED | 그 예약 삭제, 잔여1 증가 |

이는 결과 계약 표이며 DB 조회·검사 순서를 지정한 표가 아니다.
예약 ID는 DB가 생성한다. 기존 ReservationResult(status,reservationId) 반환 형식을 유지한다. 취소 입력의 두 번째 값은 userId가 아닌 reservationId다.
중심 경합의 사용자200은 신규 사용자다. 기존 사용자100의 순차 재요청도 기존 계약 보존 검사로 포함한다. 취소 재요청도 검사한다.

**실패 계약:** 테스트는 업무 처리 뒤, 테스트가 제어하는 트랜잭션의 커밋 전에 RuntimeException을 주입한다.
예외를 성공 응답으로 바꾸지 말고 호출자에게 전파한다. 삭제/생성만 남거나 좌석 변경만 남으면 실패다.
취소 실패 시 예약10은 남고 잔여0, 빈자리에서 예약 실패 시 새 예약은 없고 잔여1이다.
경합 중 취소가 실패한 경우 새 사용자가 그 미확정 좌석을 성공적으로 가져가면 안 된다.

**직접 변경할 것:** 기존 서비스와 Repository에서 새 계약에 필요한 부분만 변경한다.
현재 ReservationService는 없는 이벤트에 예외를 던진다. B003의 NOT_FOUND/null 응답에 맞추는 부분은 사용자 구현 대상으로 남겼다.
이 부분 외 기존 로직은 새로 쓰지 않는다. ORDER.md는 선택 메모이며 테스트 구현은 요구하지 않는다.
컨트롤러/HTTP API, 새 DB 설정, 별도 애플리케이션은 만들 필요 없다.
서비스에는 Repository 의존성과 @Transactional만 제공한다. 동시 실행과 장애 주입은 src/test에만 존재한다.

**진행 순서:** 먼저 순차 예약/취소 → 실패 원복 → 두 요청 겹침을 확인한다.
공개 테스트가 내부 구현 방법을 지정하지는 않는다. 내 실행 순서 설명에는
“요청 시작 / 무엇을 읽었는지 / 무엇을 바꿨는지 / 커밋 또는 롤백”을 구별해 적는다.

## 이번에 공부할 것

- **상태의 불변식:** 이벤트별 `유효 예약 수 + remaining = 1`. 개별 메서드 반환과 전체 DB 상태를 함께 판단한다.
- **원자성과 동시 실행의 차이:** 한 요청의 여러 변경을 함께 원복하는 문제와, 두 요청이 같은 상태를 보고 판단하는 문제를 구별한다.
- **트랜잭션 경계와 예외:** Spring 프록시 호출이 끝날 때 커밋/롤백이 어떻게 결정되는지 살펴본다. 기본 RuntimeException 롤백 규칙은 [Spring 문서](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)를 참고한다.
- **flush와 commit:** JPA 변경을 DB에 동기화하는 시점과 트랜잭션 완료를 구별한다. [EntityManager 문서](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/entitymanager)의 flush 부분만 참고해도 된다.
- **재현 가능한 관찰:** 필요할 때 제공 테스트의 Gate/CountDownLatch가 무엇을 기다리는지 살펴본다. 테스트 장치 구현은 AI가 담당한다. 스레드 시작 순서와 실제 DB 관찰 순서가 같은지 구별한다.

이 개념들을 모두 읽고 나서 시작할 필요는 없다. 해당 테스트에서 막힌 개념만 찾아보고 구현으로 돌아온다.
기존 선택이 혼합 경합에서도 충분한지 실행 결과와 연결해 판단한다. 격리수준 전체 비교나 교착 해결까지 확장하지 않는다.

## C003 — 새 문제: 괄호 문자열 검증

[BracketValidator.java](../../src/main/java/challenge/coding/BracketValidator.java)의 `boolean isValid(String text)`를 구현한다.
입력은 ()[]{}만 포함하며 길이0~100,000이다. 종류와 중첩 순서가 모두 올바르고 남는 괄호가 없으면 true.
빈 문자열도 true. `([]){}` → true, `([)]` → false, `((` → false.
정확한 풀이부터 구현하고 이후 O(n)을 목표로 한다. 선택한 자료구조의 이유와 복잡도를 설명한다.
구간 합 복습은 철회했으며 원본은 withdrawn에 보존했다. 추가 숙제가 아니다.

## 실행

기존 IntelliJ `study/challenge-120` 프로젝트에서 Gradle 새로고침. JDK21과 기존 Wrapper를 재사용한다.
실행 구성은 `reservation-concurrency - Backend`, `reservation-concurrency - Coding`, `reservation-concurrency - All` 및 각 `Minimum`이다.
별도 프로젝트·worktree를 만들지 않는다.

```sh
cd /Users/justin/IdeaProjects/my-project/study/challenge-120
./gradlew legacyTest --tests challenge.reservation.concurrency.MixedReservationServiceTest --rerun-tasks
./gradlew test --tests challenge.coding.BracketValidatorTest --rerun-tasks
# Minimum은 원하는 명령 뒤에 -Pminimum 추가
```

## 시간·Pass·제출

기본90분(Coding30 / Backend45 / 통합복습15), Minimum45분(15 / 20 / 10) 중 택한다.
Minimum은 순차·롤백 범위를 다루며 concurrency 태그 검사와 Coding 큰 입력1개는 제외한다. 설명은 메시지로도 받는다.
준비·설치·실행 대기는 풀이 시간에서 제외하며 실제 구현 시작부터 잰다.
통합복습은 기존 별도 복습 대화에서 진행하고 중복 문항을 추가하지 않는다.
미완성이라도 시간 안에 시도한 근거·막힌 점을 제출하면 일일 마감 가능하다.

- Coding: 괄호 종류·중첩·미완료 입력의 정확성, 본인 풀이의 복잡도와 자작 반례.
- Backend: 선택한 범위에서 응답과 최종 DB 상태가 계약에 맞는다.
- 실패: 중간 예외 후 해당 요청의 변경이 전부 원복된다.
- 설명: 관찰한 실행 순서와 상태 보호 범위를 연결한다. 기본은 제공 테스트에서 재현한 순서와 예상 결과를 설명한다.

제출은 모드·실제 시간·첫 접근·코드/테스트·짧은 설명·막힌 점·도움 범위를 간단히 적어도 된다.
제공 테스트의 경계를 독립 발견으로 세지 않고, 틀 사용은 독립성 감점 사유가 아니다.

## 환경 검증

기존 구현 재사용 후 검증 결과는 아래에 기록한다. H2만 사용하며 MySQL·성능·모든 경합 스케줄은 미검증이다.
기존 서비스의 본문은 Day 1/Day 2 원본과 동일함을 확인했다. 새 기능의 독립 구현 성과로 평가하지 않는다.
철회한 빈 틀과 관찰 장치는 withdrawn에 텍스트로 보존했으며 실행에서 제외했다.

기존 구현 연결 후 검증: JDK21 컴파일 및 H2 Backend19개 중18개 통과(인프라2 포함). 혼합 경합·원복·재요청과 서비스 자체 트랜잭션 동시 호출5회 통과. 실패1개는 missingEventReservationReturnsNotFound: 기존 reserve의 NoSuchElementException이며 환경 실패가 아니다. 사용자는 이 신규 계약 차이만 이어서 구현할 수 있다. 기존 서비스 본문 동일성·서비스의 테스트 의존성 부재·README 링크 확인. Awaitility는 테스트에만 적용했다. MySQL·실제 서버 부하는 미검증.

## 실제 MySQL과 k6 검증

서비스 코드를 변경하지 않고 Testcontainers가 일회용 MySQL 8.4 컨테이너를 띄운다.
Docker(현재 OrbStack)가 실행 중이어야 한다. 기존 사용자 DB/볼륨은 사용하지 않는다.

```sh
cd /Users/justin/IdeaProjects/my-project/study/challenge-120
./gradlew legacyTest -Pmysql --tests challenge.reservation.concurrency.MixedReservationServiceTest
python3 tools/reservation-load/run.py
```

IntelliJ 실행 구성은 `reservation-concurrency - Backend MySQL`이다. k6 실행·FD 관찰·정리는 AI가 담당한다.
[k6 스크립트](../../tools/reservation-load/mixed-reservations.js)는 60초 동안 50→200→500→1,000 VU로 올렸다가 낮춘다.
모든 VU가 정원1의 같은 이벤트를 예약하고 성공한 예약을 취소한다. 매진409는 정상 응답,
네트워크 오류/5xx/응답 계약 위반과 구별한다. 초기 기존예약 취소 후 부하 중 예약·취소가 반복해서 겹친다.
최종 잔여1·예약0, 서버 예외0, 성공 예약/취소 누계의 일치를 검사한다.

HTTP 진입점은 src/test의 LoadTestServer에만 있으며 127.0.0.1 임의 포트로 열린다.
JDK HttpServer128 worker → 실제 Spring 서비스 → Hikari 최대32연결 → MySQL 구조다.
Testcontainers는 실제 DB 동작을, JUnit/Awaitility는 순서·원복을, k6는 반복 부하의 지연/오류를 검사한다.
k6 1,000 VU가 DB 트랜잭션1,000개 동시 실행을 의미하지는 않는다. HTTP 대기와 DB 풀 대기가 포함된다.
FD 관찰은 2초 간격 표본이므로 순간 최대값은 놓칠 수 있다. 시스템 FD 설정은 변경하지 않는다.
한 머신에서 부하 생성·앱·DB를 함께 실행한 결과이며 운영 서버 용량으로 일반화하지 않는다.
JDK HTTP 어댑터는 실제 Tomcat/프록시/인증 계층의 성능을 재현하지 않는다.
산출물은 `build/load/result.json`, `k6-summary.json`, `k6-report.html`, `k6.log`, `server.log`에 저장하며 컨테이너/서버는 종료한다.
재실행 시 이전 `build/load`는 `build/load-<실행 전 시각(ns)>`로 옮겨 보존한다.

### k6 대시보드

`python3 tools/reservation-load/run.py`는 k6 내장 대시보드 데이터를 2초 간격으로 수집해
[HTML 대시보드](../../build/load/k6-report.html)를 저장한다. 종료 후에도 브라우저에서 열 수 있는 독립 HTML이다.
가상 사용자 수, 요청량, 응답시간, 오류율을 시간대별로 볼 수 있다.
FD·DB 연결 표본과 예약 불변식은 별도 `result.json`에서 확인한다.
실시간 화면도 필요하면 `K6_WEB_DASHBOARD_PORT=5665 python3 tools/reservation-load/run.py`로 실행하고
`http://localhost:5665`를 연다. 부하 종료 후에는 브라우저 연결을 닫아야 k6가 종료될 수 있다.
기본값은 포트 `-1`로 HTML만 내보내 자동 정리가 브라우저 연결을 기다리지 않게 한다.

Tomcat 설정은 변경하지 않았다. 앞선 직접 연결 실험에서 `loadServer` 실행에만
`sun.net.httpserver.maxIdleConnections=2048`을 추가했다가, Nginx 비교 실험을 위해 제거했다(현재 기본 200).
이는 요청 처리가 끝난 유휴 keep-alive 연결 보관 개수이며 worker 수나 최대 활성 연결 수가 아니다.
기존 HTTP worker 128, backlog 2048, Hikari 최대 연결 32와 OS FD 제한은 이 수정에서 바꾸지 않았다.
설정 변경 후 연결 오류가 사라졌지만 개별 소켓 추적까지 한 것은 아니므로 첫 오류의 원인 확정과 구분한다.

검증 환경: MySQL8.4.9(Testcontainers2.0.5), k6 0.56.0, JDK21, macOS/OrbStack.
확인한 MySQL 이미지 digest: `sha256:c36050afdca850f23cef85703f84c7531a5ae155a11b5ee1c60acb09937c4084`.
실제 MySQL 공개 테스트19개 중18개 통과. 실패1개는 없는 이벤트 예약의 NoSuchElementException이며 혼합 경합/롤백은 통과했다.
IntelliJ MCP로 Testcontainers 의존성이 IDE에도 동기화됐음을 확인했다.

첫 k6 실행은 `build/load-attempt-001`에 보존했다. 225,698 요청, HTTP 실패율3.417%, 서버 업무 예외0,
최종 예약1·잔여0(예약+잔여 불변식 유지), 서버 FD 표본 최대1,146/k6 1,009였다.
연결 단절로 취소 완료가 누락되어 부하 종료 계약은 실패했다. FD 고갈이나 초과 예약으로 판정하지 않는다.
JDK HttpServer 기본 유휴 연결 상한200을 확인하고 테스트 전용 실행 설정을2,048로 조정했다.
k6 gracefulRampDown 옵션도 명시적 ramping-vus scenario 아래로 옮겼다.
서비스 코드는 바꾸지 않고 같은 부하로 재검증한다. 테스트용 HTTP 계층의 제한과 DB 잠금 정확성을 구분한다.

재검증 완료: 동일한 60초·최대1,000VU 부하에서 k6 exit0, 총170,779요청(약2,841 req/s),
HTTP오류0%, 응답/종료 검사170,779회 모두 통과. HTTP전체 p95=458.13ms, 최대855.42ms.
예약 성공1,236건, 정상 매진168,304건. 취소 성공1,237건은 초기 예약 취소1건을 포함한다.
최종 예약0·잔여1, 업무 예외0, 2초 표본 불변식 위반0.
FD 표본 최대: 서버1,146/k6 1,009, DB연결 최대32. FD 고갈은 관찰되지 않았으며 OS 제한은 변경하지 않았다.
첫 실행의 연결 단절 실패와 테스트 HTTP 설정 수정 후 성공을 구별한다. 새 동시성 해법을 구현한 결과가 아니다.
실행 중 MySQL lock-wait 표에 대기 관계가 관찰됐다(첫 실행 한 시점465행, DB연결33은 진단 연결1개 포함).
이는 행 잠금 경합의 근거지만 모든 지연의 원인이 DB임을 단독 증명하지는 않는다.
위 재검증 원본: [수치](../../build/load-1789093134437785000/result.json), [k6 원본](../../build/load-1789093134437785000/k6-summary.json), [첫 실행](../../build/load-attempt-001/result.json).
테스트 서버와 MySQL 컨테이너 종료, 실행기 Python 구문·IntelliJ XML·README 링크 확인.

대시보드 수집 실행(2026-09-11 11:19 KST): 같은 60초·최대1,000VU, 104,933요청,
HTTP 오류0%, checks 전부 통과, p95 458.54ms. 예약 성공721건·매진103,488건,
최종 예약0·잔여1, 업무 예외0, 표본 불변식 위반0. FD 표본 최대 서버1,146/k6 1,009.
[당시 결과](../../build/load-1789093633519217000/result.json)와 [당시 k6 HTML 대시보드](../../build/load-1789093633519217000/k6-report.html)를 함께 확인한다.
이전 실행과 처리량이 다르므로 단일 로컬 실행의 처리량을 고정 성능 수치로 해석하지 않는다.

### JDK 기본 200 + Nginx 경유 실험

실행: `python3 tools/reservation-load/run.py --nginx`. 로컬에 설치된 Nginx를 별도 프로세스·임의 loopback 포트로 실행하고 종료한다.
JDK 유휴 연결 설정 재정의를 제거했으며 실행 중 `jcmd VM.system_properties`로 부재를 확인했다.
Nginx 1.29.1: worker 1개, worker_connections 4096, upstream `keepalive 64`, `keepalive_timeout 10s`.
HTTP/1.1과 빈 Connection 헤더로 upstream 연결을 재사용하고 `proxy_next_upstream off`로 자동 재시도를 차단한다.
클라이언트 측 keep-alive 설정은 Nginx 기본값이며, 서비스·DB 풀·JDK worker/backlog·k6 시나리오는 동일하다.
upstream 64는 유휴 연결 캐시 크기이지 활성 요청 제한이 아니다.

2026-09-11 11:27 KST 결과: 60초·최대1,000VU, 177,053요청, 약2,939 req/s, p95 363.22ms.
k6 HTTP 실패0%, checks177,053회 모두 통과. Nginx 5xx/오류 로그0.
예약 성공1,177·매진174,696·취소1,178(초기 취소1 포함), 최종 예약0·잔여1·업무 예외0·표본 불변식 위반0.
FD 표본 최대 JDK1,148/k6 1,010, MySQL 연결32. Nginx FD는 이번 표본에 포함하지 않았다.
Nginx access log의 200은2,358건, 409는174,696건이며 k6 외 준비 확인 요청1건이 포함된다.
[결과](../../build/load/result.json), [대시보드](../../build/load/k6-report.html), [실제 설정](../../build/load/nginx.conf),
[접근 로그](../../build/load/nginx-access.log), [오류 로그](../../build/load/nginx-error.log)를 보존한다.
이 구성에서 연결 오류가 재현되지 않았음을 확인했다. 프록시 추가와 연결 관리가 함께 달라졌으므로
유휴 설정 하나만의 효과나 첫 실패 원인을 단독 확정하지 않는다.
