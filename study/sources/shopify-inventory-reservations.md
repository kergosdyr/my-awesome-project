# Shopify inventory reservations — 원문 메모

- 원문: [We replaced Redis with MySQL for inventory reservations—and it scaled](https://shopify.engineering/scaling-inventory-reservations)
- 게시일: 2026-05-12
- 확인일: 2026-08-25
- 성격: Shopify Engineering의 1차 자료

## 확인한 핵심 사실

- Reserve는 결제가 시작될 때 만드는 짧은 hold이고, Claim은 결제 성공 뒤 inventory ledger에서 수량을 영구 차감하는 작업이다.
- 기존 Redis 모델은 item별 quantity key를 두고 예약 시 `DECR`, 해제 시 `INCR`했다.
- Redis 예약과 MySQL ledger claim을 하나의 원자적 연산으로 묶을 수 없어 oversell과 undersell 실패가 가능했다.
- MySQL 설계는 sellable unit 하나당 행 하나를 두고 `FOR UPDATE SKIP LOCKED`로 서로 다른 단위를 병렬 예약한다.
- 모든 재고를 행으로 만들지는 않는다. item/location 조합마다 최대 1,000개의 available row로 bounded pool을 유지한다.
- 예약이 풀을 소비하면 inventory ledger에서 replenishment한다.
- 풀이 비면 예약 요청 경로에서 인라인으로 보충한다. 하나의 트랜잭션만 보충 잠금을 획득하고 다른 요청은 기다려 thundering herd를 방지한다.
- 복합 기본키로 secondary index와 clustered index에 각각 걸리던 잠금을 한 행당 하나로 줄였다.
- 빈 풀에서 replenishment INSERT를 막는 gap lock을 피하려고 해당 트랜잭션을 `READ COMMITTED`로 실행했다.
- reserve와 claim의 테이블 잠금 순서를 통일해 deadlock 순환을 제거했다.

## 영상 링크가 있어야 확인할 내용

- Redis 예약 만료를 Sorted Set으로 관리했는가?
- 10초 주기의 cron cleanup을 사용했는가?
- Redis 예약 경로의 Lua 스크립트가 정확히 어떤 명령을 원자화했는가?
