# B009 — 주문이 계속 들어오는 목록, 어디부터 이어 읽을까?

Day9 · Backend45분 / Minimum20분. [과제 목록](README.md)

## 오늘 겪을 사건과 완료 기준

운영자가 최신 주문을 3개씩 읽는다. 첫 화면에서 `[106,105,104]`를 읽은 사이 주문107이 들어왔다. 다시 ‘앞에서3개를 건너뛰기’로 읽으면 `[104,103,102]`가 되어104를 또 본다. 한편 주문2만 개 중18,001번째부터20개를 읽을 때, DB가 정말20개만 살펴보는지도 확인하고 싶다.

1. **문제:** 새 주문이 들어와도 이미 보던 주문을 이어 읽고, 다품목·결제 정보까지 빠짐없이 반환한다.
2. **이유:** SQL이2번이라고 가벼운 요청은 아니다. 가져온 주문·품목 수와 응답 크기도 비용이다.
3. **개념:** 다음 위치를 표현하는 표식, DB에서 건수 제한, 인덱스와 실행계획.
4. **완료:** 아래 공개 결과 검사 통과, 전체 주문을 애플리케이션으로 읽지 않는 조회, 같은 데이터에서 선택 전후의 계획 비교와 선택 이유 설명. 밀리초 합격선·특정 SQL 횟수는 없다.

먼저 판단할 것: **다음 요청에 ‘몇 개 읽었는지’와 ‘마지막에 무엇을 읽었는지’ 중 어떤 정보를 남길까?** 같은 시각의 주문이 여러 개라면 충분한 정보인지도 예상해 본다. 정답 표식 형식·조회 쿼리·인덱스 열 순서는 직접 정한다.

## 입력과 반환

제공된 새 진입점: `GET /api/orders/window?size=3&after=...`. after를 생략하면 첫 페이지다. 기존 `GET /api/orders`와 FORM 화면은 비교용 전체 조회로 유지한다. 오늘은 화면 변경 없이 같은 주문 데이터의 새 API를 구현한다.

정렬은 **createdAt 내림차순, 같은 시각이면 id 내림차순**이다. 기존 전체 API는 id 내림차순이므로 이번 API의 기준과 구분한다. 데이터에는 ID가 더 커도 시각은 앞선 주문과 같은 시각의 주문이 섞여 있다.

응답은 `{ "entries": [기존 OrderDetailsResponse], "next": "직접 정한 표식 또는 null" }`이다. 클라이언트는 next를 해석하지 않고 URL 인코딩해서 다음 after로 전달한다. 주문·품목·결제 상태의 기존 응답 모양을 보존한다.

| 상황 | 결과 |
| --- | --- |
| 주문 없음 | entries=[], next=null |
| 정렬된 ID가106~101이고 size=3 | 첫106,105,104 / 다음103,102,101, 마지막 next=null |
| 첫 응답 뒤107을 최신 시각으로 추가 | 이어 읽기는103,102,101. 새 첫 요청은107부터 |
| 총5개, size=3 | 첫3개 / 마지막2개, next=null |
| 정확히6개, size=3 | 두 번째3개에 next=null. 빈 세 번째 페이지를 요구하지 않음 |
| 같은 after·size 재요청 | 기존 행이 그대로이고 최신 삽입만 있으면 같은 항목·next |
| 같은 시각 주문 여러 개, size=1 | ID 내림차순으로 하나씩, 중복·누락 없음 |
| 결제 없음 / 진행 중 / 완료 | UNPAID / PENDING / PAID 및 기존 승인 ID 유지 |
| size가1~100 밖 | 제공된400 검증 사용 |

오늘 입력 가정: after는 이 API가 발급한 유효한 값이며 이어 읽는 동안 size는 같다. 기존 주문의 수정·삭제·과거 시각 삽입·표식 위조·만료·전체 시점 스냅샷·아무 페이지로 바로 이동·전체 건수는 제외한다. 신규 주문은 이전 모든 주문보다 **더 최신 시각**이다. 동시에 요청을 날리는 부하 시험이 아니라 ‘응답→새 주문 커밋→다음 요청’ 순서로 재현한다.

## 제공한 것과 직접 할 일

제공: 기존 주문/품목/결제 모델, HTTP 입력·응답 매핑, `OrderWindowResult`, 테스트 DB 자동 시작, 결과 중심 HTTP 테스트7개, 주문20,000·품목40,000·결제 혼합 데이터, 실행계획·측정 도구. 설치·대량 주문 생성·측정기 작성은 숙제가 아니다. 개발 서버 데이터는 건드리지 않는다.

직접 구현할 메서드:

- [OrderQueryService.window](../../commerce/src/main/java/challenge/commerce/domain/order/OrderQueryService.java): size와 after를 읽어 해당 주문 구간을 찾는다. 그 주문의 품목·결제를 조합해 entries와 next를 반환한다. 조회 작업이므로 주문·결제 상태는 바꾸지 않는다. 현재 본문은 TODO다.
- 필요에 따라 [OrderReader](../../commerce/src/main/java/challenge/commerce/domain/order/OrderReader.java)·[OrderRepository](../../commerce/src/main/java/challenge/commerce/domain/order/OrderRepository.java)에 구간 조회 메서드를 추가한다. 받아야 할 경계 값과 반환할 대상은 직접 정한다.
- [OrderRepositoryImpl](../../commerce/src/main/java/challenge/commerce/infra/db/OrderRepositoryImpl.java)·[OrderJpaRepository](../../commerce/src/main/java/challenge/commerce/infra/db/OrderJpaRepository.java)에서 정렬·필터·건수 제한을 DB에 전달한다. 조회한 모든 주문을 Java에서 잘라 반환하는 구현은 효율성 완료가 아니다.
- [b009-index.sql](../../commerce/src/test/resources/b009-index.sql)에 후보 인덱스 하나를 `b009_candidate`라는 이름으로 작성한다. 도구가 적용 전후를 측정하고 제거한다. 거절한 후보도 관찰 근거를 설명하면 된다. 운영용 인덱스 반영은 오늘 범위 밖이다.
- [b009-query.sql](../../commerce/src/test/resources/b009-query.sql)에 **자신이 구현한 경계 조회와 같은 SELECT**를 적는다. 첫 위치·깊은 위치를 세미콜론으로 구분한다. `${boundaryId}`, `'${boundaryTime}'`은 도구가18,000번째 주문의 실제 값으로 채운다. SQL 자체가 설계 대상이며 측정 장치는 이미 제공했다.

쿼리·EntityManager·JpaRepository는 infra에 둔다. 서비스가 트랜잭션을 소유하며 JPA Entity 공유 예외를 그대로 사용한다. 응답은 DTO, OSIV=false. 기존 전체 조회·결제·가격 검증을 고치거나 결함을 넣지 않는다.

## 바로 실행

`study/challenge-120`에서 Java21과 기존 Gradle Wrapper만 사용한다. Docker·MySQL 설치가 필요 없다.

```sh
# 구현 전에도 성공하는 관찰: 삽입 사건, 현재 전체 API 비용, 얕은/깊은 OFFSET 계획
./gradlew :commerce:pagingExperiment
# 제공 데이터 자체 확인 (TODO와 무관하게 통과)
./gradlew :commerce:test --tests '*OrderPagingFixtureTest'
# 구현 결과 검사: TODO 상태에서는 실패가 정상이다
./gradlew :commerce:test --tests '*OrderWindowTest'
# 자신의 API 구현 후: 동일 대량 데이터에서 size20/100의 SQL·entityLoad·응답 크기도 출력
./gradlew :commerce:pagingExperiment -Pb009Solution
# Minimum: 빈 목록·첫 페이지/재요청·품목/결제의3개
./gradlew :commerce:test --tests '*OrderWindowTest' -Pminimum
```

관찰 출력: `commerce/build/b009-observation.txt`. IntelliJ: `B009 - Observe`, `B009 - Order Paging`.

기본 관찰은 조회 방식의 문제를 재현할 뿐 **풀이 성공 검사와 별개**다. 후보 SQL/인덱스 파일이 비어 있으면 ‘미측정/미선택’을 출력하며 완료로 표시하지 않는다. SQL 파일은 테스트 DB에서만 실행한다.

## 45분 안에서 선택·구현·관찰

- 0~5분: 삽입 사건 예상, 다음 요청에 필요한 정보와 정렬 동률 처리 결정.
- 5~30분: 구간 조회·품목/결제 조합 구현, 결과 테스트 실행. SQL 수2회를 억지로 유지할 필요는 없다.
- 30~40분: 후보 SQL·인덱스를 넣고 도구 실행. 첫/깊은 조회의 반환 행 수·scanCount·선택된 인덱스·정렬 여부를 비교.
- 40~45분: 선택한 표식으로 보장하는 것, 인덱스로 줄어든/남은 작업, 임의 페이지 이동의 비용을 각 한 문장으로 설명.

Minimum20분은 첫 페이지3개 검사와 기본 관찰까지다. 이어 조회·인덱스 구현까지 정규 완료로 판정하지 않는다. 이후 확장은 사용자 요청 때 이어가며 자동으로 밀린 숙제를 쌓지 않는다. Coding30/통합 복습15분은 별도 기존 예산이며 오늘 복습은 이미 발행·응답되어 새로 만들지 않는다.

## 측정 해석과 주의점

H2 실제 테이블을 사용한다. ORM 전체 조회의 SQL 수·엔티티 적재 수와 HTTP 응답 바이트, 별도 SQL의 `EXPLAIN ANALYZE`를 출력한다. JDBC 시간은 워밍업3회 뒤7회 중앙값이고 결과 캐시를 끈다. HTTP 시간은 단회라 성능 비교 결론에 사용하지 않는다. SQL별 컬럼·반환량이 다르면 같은 작업의 개선율로 계산하지 않는다.

H2의 scanCount는 해당 엔진이 기록한 행 접근 관찰값이다. 디스크 페이지 읽기 수·InnoDB 버퍼 적중·운영 동시 부하·MySQL 실행계획을 보장하지 않는다. 인덱스가 없으면 첫 페이지와 깊은 페이지 모두 전체 스캔으로 같게 나올 수도 있다. ‘깊은 쪽 시간이 무조건 길어야 성공’이 아니다. [H2 실행계획 설명](https://h2database.com/html/commands.html#explain), [H2 성능 관찰](https://h2database.github.io/html/performance.html).

품목 컬렉션 fetch join에 건수 제한을 붙였을 때 DB 제한이 실제 적용되는지 확인한다. Hibernate는 조합에 따라 전체 결과를 읽어 메모리에서 제한할 수 있다. 경고·생성 SQL·entityLoad를 함께 읽고 판단한다. 결과 테스트만으로 DB 효율성을 입증하지 않는다. [Hibernate fetch join과 pagination](https://docs.jboss.org/hibernate/orm/7.0/querylanguage/html_single/Hibernate_Query_Language.html).

과거9/14에 주문 이어 조회를 발행했다가 결제 학습 유지 요청으로 철회한 이력이 있다. 그것을 완료 과제로 간주하거나 삭제한 코드를 복원하지 않았다. 이번에는 사용자의 기술 중심 과제 요청에 따라 **현재 다품목 FORM의 응답 완전성과 DB 비용 비교**를 함께 다룬다.
