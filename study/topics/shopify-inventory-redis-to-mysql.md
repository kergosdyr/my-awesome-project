---
title: "Shopify 재고 예약: Redis에서 MySQL로"
status: active
review_status: scheduled
created: 2026-08-25
last_reviewed: 2026-08-25
next_review: 2026-08-26
interval_days: 1
review_count: 0
blog_seed: maybe
tags: [inventory, reservation, redis, mysql, concurrency, skip-locked]
sources:
  - "Shopify 재고 관리 시스템 관련 YouTube 영상 — 링크 필요"
  - "../sources/shopify-inventory-reservations.md"
sessions:
  - "../sessions/2026-08-25.md"
---

# Shopify 재고 예약: Redis에서 MySQL로

## 현재 상태

첫 회상 뒤 Shopify Engineering 원문을 찾아 일부 내용을 대조했다. Sorted Set, Lua, 10초 cron은 원문에 없으므로 영상 링크를 받기 전까지 미확인으로 남긴다.

## 회상에서 복원한 흐름

1. Redis에서 구매 전 재고를 예약한다.
2. 만료 시각이 있는 세션을 Sorted Set으로 관리한다.
3. 구매 완료 시 MySQL의 실제 재고를 차감한다.
4. 만료됐지만 아직 청소되지 않은 예약 때문에 일시적인 품절 표시가 생길 수 있다.
5. Redis–MySQL 간 일관성 부담을 줄이기 위해 MySQL 중심 설계로 이동한다.
6. MySQL에서는 한 재고 행의 경합을 피하려고 여러 행과 `SKIP LOCKED`, `LIMIT 1`을 사용한다.

## 아직 답하지 못한 설계 질문

- Redis에 저장한 값과 MySQL에 저장한 값의 정확한 의미와 불변식은 무엇인가?
- 예약의 생성·만료·구매 확정을 어떤 원자적 단위로 처리하는가?
- 만료 청소가 늦을 때 정확성 문제와 가용성 문제 중 어느 것이 발생하는가?
- 여러 MySQL 행과 1,000개 상한은 재고 표현인가, 경합 분산용 버퍼인가?
- MySQL 전환 뒤에도 Redis가 남는다면 어떤 역할만 담당하는가?

## 현재까지 복원한 불변식 후보

Redis 숫자를 `지금 새 예약에 내줄 수 있는 수량`으로 정의한다면 다음 관계가 후보가 된다.

`Redis 판매 가능 수량 = MySQL 실제 재고 - 아직 유효한 미구매 예약 수`

단, 이 관계가 즉시 항상 성립하는지, 만료 청소 지연을 허용해 보수적으로 작아질 수 있는지는 원문 확인이 필요하다.

만료 청소 전에는 Redis가 이미 이탈한 예약까지 활성으로 간주해 이 식보다 작은 값을 보일 수 있다. 이 차이는 초과 판매를 허용하기보다 판매 가능한 재고를 숨기는 `거짓 품절`로 나타난다.

구매 시점의 재검사만으로는 Redis의 품절 판단 때문에 구매 흐름에 들어오지 못한 고객을 구제할 수 없다. 따라서 만료 예약 회수는 적어도 새 예약을 거절하기 전의 경로에도 필요하다는 설계 가설을 세웠다. 원문의 실제 구현은 아직 미확인이다.

새 예약 경로의 가설은 `판매 가능 수량 확인 → 부족하면 만료 예약 회수 → 수량 재확인 → 차감과 예약 등록`이다. 이 연산을 여러 Redis 명령으로 분리하면 확인과 갱신 사이에 다른 요청이 끼어드는 check-then-act/read-modify-write 경쟁이 생기므로, 하나의 Lua 실행으로 묶어야 한다. 화면에 표시한 수량은 참고값이고 실제 재고 소유권은 예약 성공 응답에서 확정한다.

원자성이 깨지면 두 요청이 같은 만료분을 각각 회수해 더블 부킹할 수 있다. 따라서 `한 재고 단위에는 동시에 최대 하나의 활성 예약만 존재한다`가 핵심 불변식이며, 집계로는 `완료된 판매 수 + 활성 예약 수 <= 확보 가능한 전체 재고`라고 표현할 수 있다.

## MySQL 경합 분산

재고 단위를 여러 행으로 표현하고 `SELECT ... FOR UPDATE SKIP LOCKED LIMIT 1`을 사용하면, 한 요청이 잠근 행을 다른 요청이 기다리지 않고 건너뛰어 다음 잠기지 않은 후보를 선택할 수 있다. 후보가 모두 잠겼거나 소진됐다면 에러가 아니라 선택 결과가 없을 수 있다.

단일 카운터 행의 조건부 쓰기(`quantity > 0`)는 음수 재고를 막아 정확성을 지키지만, 모든 요청이 같은 행의 잠금을 거쳐야 하므로 hot-row 경합 자체를 없애지는 않는다. 여러 후보 행과 `SKIP LOCKED`가 요청을 서로 다른 잠금으로 분산해 대기 시간을 줄인다.

`SELECT ... FOR UPDATE`와 `UPDATE`는 모두 대상 행에 쓰기 잠금을 잡는다. `SKIP LOCKED`는 잠금을 만드는 독립 기능이 아니라 잠금 읽기가 이미 잠긴 행을 기다리지 않고 결과에서 제외하게 하는 옵션이다. 따라서 단일 카운터의 원자적 조건부 `UPDATE`만으로 초과 판매는 막을 수 있지만, 한 행에 모이는 잠금 대기는 그대로다. `quantity=100`이면 요청들이 직렬화되더라도 조건을 통과하는 앞의 100개 요청은 하나씩 성공할 수 있고, 이후 요청부터 영향받은 행이 0개가 된다.

예를 들어 `quantity=2`에 세 요청이 동시에 오면 앞의 두 요청은 각각 하나씩 차감해 성공하고 마지막 요청은 조건 불충족으로 실패하며 최종 수량은 0이다. 조건부 쓰기는 음수 재고를 막지만 세 요청의 잠금 경로는 같은 단일 행에 집중된다.

재고 2개를 별도 행으로 표현하면 D와 E가 서로 다른 행 하나씩을 동시에 잠글 수 있다. D가 첫 행을 잠근 동안 E는 `SKIP LOCKED`로 첫 행을 건너뛰어 둘째 행을 선택하고, F는 남은 후보가 없어 빈 결과를 받는다. 잠긴 행의 총수는 둘이지만 요청들이 같은 잠금을 순서대로 기다리지 않는 것이 차이다.

## 원문으로 확인한 bounded pool

- 하나의 행은 수량 버킷이 아니라 예약 가능한 sellable unit 하나다.
- 모든 실제 재고를 행으로 만들지 않고 item/location 조합별로 최대 1,000개의 available row만 유지한다.
- 예약은 풀의 행을 소비하고 replenishment process가 inventory ledger를 기준으로 다시 채운다.
- 풀이 비면 예약 경로에서 인라인 보충한다. 한 트랜잭션만 보충 잠금을 잡고, 나머지 요청은 함께 INSERT하지 않고 기다린다.
- 이 대기는 해당 요청의 지연을 늘리지만 실제 재고가 있는데 품절로 돌려보내는 것을 피한다.

Inventory ledger는 실제 재고의 source of truth다. Bounded pool 보충은 판매가 아니라 예약 가능한 unit row를 materialize하는 작업이므로 보충 개수만큼 ledger를 영구 차감하지 않는다. 하나의 요청이 item/location 보충 잠금을 잡고 풀을 채워 커밋하면, 기다리던 요청들은 `SKIP LOCKED`를 다시 실행해 서로 다른 unit을 가져간다. Ledger의 영구 차감은 결제가 성공해 reservation을 claim할 때 발생한다.

1,000 상한은 활성 예약과 pool의 합이 아니라 item/location별 available pool 행 수의 상한이다. 예를 들어 ledger 50,000, 활성 예약 200, 빈 pool이라면 `200 + 1,000 <= 50,000`이므로 pool을 1,000행까지 채울 수 있다. 다만 실제 보충량은 pool 목표치뿐 아니라 아직 예약·판매로 약속되지 않은 ledger 재고를 넘지 않아야 한다.

## 격리 수준 비교 메모

- 격리 수준은 스냅샷을 언제 고정하는지와 동시 충돌을 기다림, 범위 잠금, 재시도 중 무엇으로 처리하는지를 함께 봐야 한다.
- MySQL InnoDB 기본은 `REPEATABLE READ`다. 일반 SELECT는 첫 consistent read의 transaction snapshot을 재사용하지만, 범위 locking read는 phantom을 막기 위해 record와 gap을 합친 next-key lock을 사용할 수 있다.
- MySQL `READ COMMITTED`는 일반 SELECT마다 새 snapshot을 만들고, gap lock 사용을 줄여 빈 pool을 채우는 INSERT와의 충돌을 피할 수 있다.
- PostgreSQL 기본은 `READ COMMITTED`이며 문장마다 snapshot을 만든다. PostgreSQL의 `REPEATABLE READ`는 snapshot isolation으로 transaction snapshot을 유지하고, `SERIALIZABLE`은 predicate dependency를 감지해 위험한 트랜잭션을 실패·재시도시키는 방식을 사용한다.
- Oracle 기본도 `READ COMMITTED`이며 undo를 이용한 statement-level snapshot을 제공한다. Oracle은 일반적인 `REPEATABLE READ` 이름 대신 `SERIALIZABLE` 또는 read-only transaction으로 transaction-level snapshot을 제공한다.
- 따라서 `REPEATABLE READ라서 무조건 gap lock`이라고 일반화하면 안 된다. Shopify 문제는 MySQL InnoDB의 locking read와 next-key/gap lock 구현에 관한 것이다.

### Shopify 빈 pool 문제

MySQL `REPEATABLE READ`에서 빈 pool 범위를 `FOR UPDATE SKIP LOCKED`로 읽으면 반환할 행은 없어도 “이 범위에 행이 없음”을 지키기 위해 마지막 index record 뒤의 supremum gap을 잠글 수 있다. Replenishment가 같은 범위에 unit row를 INSERT하려면 이 gap과 충돌해 기다리며, 다른 잠금 의존성까지 있으면 deadlock cycle이 될 수 있다. Shopify는 이 트랜잭션을 `READ COMMITTED`로 실행해 해당 gap-lock 충돌을 피했다.

### Gap lock 직관

B-tree index에 `item_id=41`과 `item_id=43`만 있다면 `item_id=42`는 실제 행이 아니라 두 키 사이의 빈 자리다. MySQL의 next-key locking은 `item_id=42 FOR UPDATE` 결과가 비어 있어도 이 빈 구간에 새 키가 들어오지 못하게 할 수 있다. 그렇지 않으면 같은 locking read를 다시 실행했을 때 다른 트랜잭션이 INSERT한 42가 갑자기 나타나는 phantom이 생긴다. `SKIP LOCKED`는 이미 잠긴 결과 행을 제외하는 옵션이므로, 반환할 행이 없고 빈 구간 자체가 잠긴 상황에서는 INSERT 충돌을 없애지 못한다.

Phantom은 무조건 잘못된 결과가 아니다. `READ COMMITTED`에서는 문장마다 새 snapshot을 보므로 다른 트랜잭션이 커밋한 새 행을 다음 조회에서 보는 것이 정상이다. Shopify의 빈 pool 경로도 replenishment가 넣은 행을 다음 예약 시도가 봐야 한다. 따라서 MySQL RR의 gap-lock 보호는 이 업무에서는 불필요한 방해였고, `READ COMMITTED`로 낮춘 뒤 동시 replenishment는 별도의 잠금으로 직렬화했다.

SQL 표준의 `REPEATABLE READ`는 non-repeatable read를 막지만 phantom까지 반드시 막는 최소 보장은 아니다. PostgreSQL RR은 snapshot isolation으로 phantom을 보이지 않고, MySQL InnoDB도 consistent read와 locking range의 next-key lock을 통해 더 강한 동작을 제공한다. 따라서 격리 수준 이름만으로 범위 잠금 구현을 단정하면 안 된다.

`READ COMMITTED`에서 빈 pool을 `SELECT FOR UPDATE`하면 조정용으로 잠글 실제 pool 행이 없다. Replenishment를 하나로 직렬화하려면 item/location마다 항상 존재하는 inventory ledger 행이나 별도의 `replenishment_guard` 행을 먼저 `FOR UPDATE`로 잠근다. 잠금을 기다린 요청은 획득한 뒤 pool을 다시 확인하고, 선행 요청이 이미 보충했다면 INSERT하지 않고 예약 단계로 진행한다.

Guard 잠금을 기다리기 전의 pool 조회 결과는 잠금 획득 시점에는 이미 오래된 값일 수 있다. 따라서 `확인 → guard 잠금 → 재확인 → 조건부 보충` 순서를 사용해야 한다. 재확인을 생략하면 두 요청이 같은 item/location에 각각 1,000행을 넣는 중복 보충이 가능하다.

## Clustered primary key와 잠금 수

InnoDB의 primary-key B-tree leaf는 전체 행 자체를 저장하는 clustered index다. Secondary-index leaf는 secondary key와 해당 행의 primary-key 값을 저장한다. 따라서 auto-increment `id`가 PK이고 `(shop_id, item_id, inventory_group_id)`로 조회하면 secondary record를 찾은 뒤 그 안의 `id`로 clustered record를 다시 찾아야 한다. Locking read는 secondary index entry와 실제 clustered record를 모두 보호할 수 있다. Shopify는 필터 열과 `id`를 `(shop_id, inventory_item_id, inventory_group_id, id)` composite primary key로 만들어 하나의 clustered index 경로에서 조회와 실제 행 잠금을 끝냈고, 관찰된 잠금 수를 행당 두 개에서 하나로 줄였다.

Secondary entry만 잠그면 동일 행을 PK로 직접 접근하는 다른 트랜잭션과 공통 잠금 지점이 생기지 않는다. 예를 들어 T1이 secondary entry `(shop=1,item=42,location=7) → id=101`만 잠근 상태에서 T2가 `UPDATE ... WHERE id=101`로 clustered record를 직접 찾으면 두 트랜잭션이 서로 다른 물리 record를 잠그게 된다. 실제 행의 변경을 직렬화하려면 최종 clustered record가 공통 잠금 지점이어야 한다. Composite PK에서는 검색 entry 자체가 전체 행을 가진 clustered record라서 secondary와 clustered record 두 곳을 따로 잠글 필요가 없다.

Index entry는 B-tree leaf page에 정렬되어 저장된 index record 한 건이다. `idx_lookup(shop_id,item_id,location_id)`에 속한 한 행의 PK가 `id=101`이면 InnoDB secondary leaf에는 개념적으로 `(shop_id=1,item_id=42,location_id=7,id=101)`이 저장된다. 마지막 `id`는 포인터가 아니라 clustered PK B-tree에서 실제 행을 다시 찾기 위한 PK 값이며, 동일한 secondary key를 가진 여러 행을 구분하는 역할도 한다.

“T1이 secondary entry만 잠근다”는 설명은 실제 동작이 아니라 왜 그것만으로 부족한지를 보이는 가정이다. 그 가정에서는 T1이 secondary record S를, PK로 접근한 T2가 clustered record C를 잠가 잠금 대상이 다르므로 서로 기다리지 않고 동시에 진행할 수 있다. 실제 InnoDB는 secondary-index locking read 때 clustered record C도 잠가 공통 충돌 지점을 만들며, T2의 PK UPDATE는 C에서 기다린다.

## 블로그 원재료 후보

- “재고 차감”과 “구매 전 예약”을 같은 카운터로 생각할 때 생기는 혼동
- TTL 또는 주기적 청소가 곧바로 예약 복구의 정확성을 보장하지 않는 이유
- 단일 hot row 대신 `SKIP LOCKED` 가능한 여러 행을 두는 설계의 비용
- 두 데이터 저장소 사이의 불변식을 제거하거나 줄이는 것이 구조 단순화에 미치는 영향
