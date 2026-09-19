# B009 — 새 주문이 들어와도 목록을 중복 없이 이어 읽기

[과제 목록](README.md) · 기존 Backend45분 / Minimum20분 범위. 시간 안에 끝내는 시험은 아니다.

## 해결할 상황

운영자가 최신 주문을3개씩 확인한다.

```text
전체 주문: 106 105 104 103 102 101
첫 화면:  106 105 104
```

그 사이 최신 주문107이 들어왔다. 기존 페이지 방식으로 앞의3개를 건너뛰면 다음 화면에104가 다시 나온다.

```text
바뀐 목록: 107 106 105 104 103 102 101
앞의3개를 건너뛴 화면: 104 103 102
원하는 다음 화면:     103 102 101
```

**이미 읽은 마지막 주문을 기준으로 다음 주문을 찾는 조회를 구현한다.** 새 주문107은 처음부터 새로 요청할 때 보이면 된다. 위 예시는 번호가 큰 주문이 최신이지만, 실제 정렬은 **createdAt 내림차순 → 같은 시각이면 id 내림차순**이다.

## API 규격 — 이미 결정된 제공 사항

첫 요청에는 개수만 보낸다.

```http
GET /api/orders/window?size=3
```

응답의 entries에는 기존 주문 상세 정보(모든 품목·결제 상태 포함)가 들어간다. next는 다음 요청에 사용할 두 값을 담는다. 아래는 entries의 상세 필드를 생략한 설명이다.

```text
entries: 주문106,105,104의 상세 정보
next: { "createdAt": "2026-09-18T10:30:00Z", "id": 104 }
```

다음 요청은 next의 두 값을 각각 전달한다.

```http
GET /api/orders/window?size=3&afterCreatedAt=2026-09-18T10:30:00Z&afterId=104
```

- `afterCreatedAt`: 마지막으로 본 주문 시각. UTC ISO 8601 형식이며 서버는 `Instant`로 받는다. 소수 초가 있다면 그대로 전달한다.
- `afterId`: 그 주문의 ID. 서버는 정수로 받는다.
- 두 값 모두 없으면 첫 요청이다. 하나만 있거나 날짜·ID가 잘못되면 제공 코드가400으로 처리한다.
- `size`: 기본20, 최대100. 제공 코드가 범위를 검증한다.
- 서비스 입력은 `window(int size, OrderCursor after)`다. `OrderCursor`는 `Instant createdAt`, `long id`를 가진 제공 타입이며 첫 요청에서는 after=null이다.
- 더 읽을 주문이 있으면 next에 **이번 응답 마지막 주문의 시각과 ID**를 넣는다. 끝이면 next=null이다.

문자열 인코딩·구분자·파싱을 설계하는 일은 없다. FORM 화면은 완성된 `/api/orders?page=0&size=20`을 계속 사용한다. 새 API는 HTTP 테스트로 호출하며 화면 변경은 과제가 아니다.

## 직접 작성할 부분

| 위치 | 할 일 | 반환·상태 변경 |
| --- | --- | --- |
| [OrderQueryService.window](../../commerce/src/main/java/challenge/commerce/domain/order/OrderQueryService.java) | size와 after를 Reader에 전달하고 조회 결과를 응답에 연결한다 | `OrderWindowResult(entries, next)`. 조회한 주문·결제 상태는 변경하지 않는다 |
| [OrderReader](../../commerce/src/main/java/challenge/commerce/domain/order/OrderReader.java)·[OrderRepository](../../commerce/src/main/java/challenge/commerce/domain/order/OrderRepository.java) | 이어 조회 메서드를 연결한다 | 시각·ID·개수를 타입 그대로 전달한다. SQL은 작성하지 않는다 |
| [OrderRepositoryImpl](../../commerce/src/main/java/challenge/commerce/infra/db/OrderRepositoryImpl.java) | Q클래스로 다음 주문을 찾는 조건, 정렬, DB 조회 제한을 작성한다. 다음 데이터 유무를 판단한다 | 해당 주문과 모든 품목·결제를 기존 JOIN 방식으로 읽는다 |
| [b009-query.sql](../../commerce/src/test/resources/b009-query.sql) | 제공된 첫 페이지·이어 조회 SELECT를 사용한다 | 첫 위치와 깊은 위치의 실행계획 비교용 |
| [b009-index.sql](../../commerce/src/test/resources/b009-index.sql) | 도움이 될 것으로 예상하는 인덱스를 `b009_candidate` 이름으로 작성한다 | 테스트 DB에서만 적용 전후 비교. 운영 반영은 제외 |

기본 페이지네이션·Entity JOIN·DTO·입력 검증·데이터 생성·측정 도구는 제공 코드다. **직접 설계할 핵심은 다음 주문을 고르는 조회 조건과 그 조회에 맞는 인덱스다.** 같은 시각에 여러 주문이 있을 때도 이어 읽을 수 있어야 한다. 조회 코드 작성 이후 사용자 요청에 따라 측정용 SELECT는 제공했다. 남은 실험에서는 인덱스만 직접 설계한다.

전체 주문을 읽어서 Java에서 잘라내면 안 된다. 주문3개는 품목3행이 아니다. 한 주문의 품목이 여러 개여도 모두 반환한다. 기존 findPage의 ‘ID 제한 조회 → 해당 주문 상세 JOIN’ 구조는 참고할 수 있다.

## 결과가 맞는지 확인할 사례

| 상황 | 기대 결과 |
| --- | --- |
| 주문 없음 | entries=[], next=null |
| 최신순106~101, size=3 | 첫106,105,104 / 다음103,102,101 |
| 첫 응답 뒤 최신107 추가 | 다음은 여전히103,102,101. 새 첫 요청은107부터 |
| 총5개, size=3 | 첫3개 / 마지막2개, next=null |
| 정확히6개, size=3 | 두 번째3개에서 next=null. 빈 세 번째 요청 불필요 |
| 같은 입력으로 재요청 | 기존 행이 그대로이고 최신 삽입만 있다면 같은 항목과 next |
| 같은 시각에 여러 주문, size=1 | ID 내림차순으로 중복·누락 없이 조회 |
| 결제 없음 / 진행 중 / 완료 | 기존 UNPAID / PENDING / PAID와 승인 ID 유지 |

기존 주문의 수정·삭제·과거 시각 삽입은 다루지 않는다. 다음 요청에는 서버가 반환한 시각·ID를 그대로 보내고 size는 유지한다. 새 주문은 기존 주문보다 최신 시각이다. 페이지 번호로 바로 이동, 전체 건수, 조회 시작 시점의 완전한 스냅샷도 범위 밖이다.

## 인덱스 실험은 무엇을 알아보려는가?

제공 데이터는 주문20,000개·품목40,000개다. 기존 방식으로18,000개를 건너뛰고20개를 읽는 경우와 직접 구현한 이어 조회를 관찰한다. **20개만 반환해도 DB가 찾는 동안 훨씬 많은 행을 살펴볼 수 있다.**

제공된 SELECT에 후보 인덱스를 적용하기 전후로 다음을 확인한다.

1. 결과가 의도한 주문 순서와 개수인가?
2. 어떤 인덱스를 사용했는가? 행 접근 관찰값(scanCount)과 정렬 작업은 어떻게 달라졌는가?
3. 선택한 인덱스가 왜 도움이 됐거나 도움이 안 됐는가?

SQL 파일의 `${boundaryId}`, `'${boundaryTime}'`에는 도구가18,000번째 주문의 실제 값을 넣는다. 첫 위치·깊은 위치 SELECT는 세미콜론으로 구분한다. 쿼리의 컬럼·반환량이 다르면 실행 시간을 같은 작업의 개선율로 비교하지 않는다. 인덱스가 효과가 없었다면 관찰 근거를 설명해도 된다. 특정 밀리초·SQL 횟수 합격선은 없다.

## 실행과 완료 기준

`study/challenge-120`에서 실행한다. Java21·Gradle Wrapper를 사용하며 Docker/MySQL 설치는 필요 없다.

```sh
# 구현 전: 기존 방식의 중복 사건과 얕은/깊은 위치 조회 관찰
./gradlew :commerce:pagingExperiment
# 제공된 HTTP 입력·응답 규격 검사: 풀이 미완성이어도 통과
./gradlew :commerce:test --tests '*OrderWindowContractTest'
# 자신의 조회 구현 검사: 현재7개 통과
./gradlew :commerce:test --tests '*OrderWindowTest'
# 구현 후: 자신의 API 비용과 SQL·인덱스 관찰
./gradlew :commerce:pagingExperiment -Pb009Solution
# Minimum: 빈 목록·첫 페이지/재요청·품목/결제3개만 검사
./gradlew :commerce:test --tests '*OrderWindowTest' -Pminimum
```

관찰 결과: `commerce/build/b009-observation.txt`. IntelliJ: `B009 - Observe`, `B009 - Order Paging`.

정규 완료는 기존 결과 검사7개 통과, DB 조회량 제한, 조회·인덱스 적용 전후 관찰과 선택 이유 설명이다. 입력 변환 검사는 제공 환경의 책임이며 사용자 완료 조건에 추가하지 않는다. Minimum20분은 위3개 검사와 기본 관찰까지이고 전체 완료와 구분한다. 기존45분 예산을 늘리거나 추가 숙제를 붙이지 않는다.

이 과제는 조회·인덱스에 집중하는 작은 실험이다. 서비스 전체를 처음부터 설계해 보는 연습은 아니다. 제공 코드를 따라 채운 것만으로 설계 능력을 익혔다고 평가하지 않는다.

측정은 H2에서 이루어진다. scanCount는 해당 엔진의 행 접근 관찰값이며 MySQL/InnoDB의 디스크 I/O나 운영 성능을 증명하지 않는다. [H2 실행계획 설명](https://h2database.com/html/commands.html#explain).

## 작성 중 코드 보존

- [기본 페이지 수정 전 원본](../attempts/order-paging/B009-before-baseline-fix.java.txt)
- [타입 입력 변경 전 원본](../attempts/order-paging/B009-before-typed-input.java.txt): 작성 중이던 `orderReader.readWindow()` 줄을 포함한 파일 전체. 입력 규격을 바꾸면서 실행 환경이 컴파일되도록 당시 본문은 TODO 예외로 두었다. 이후 사용자가 조회를 구현했고, 조건식 수정과 리뷰 피드백을 거쳐 결과 검사7개를 통과했다.
