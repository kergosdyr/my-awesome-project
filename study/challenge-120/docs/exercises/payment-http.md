# Challenge Day 6/120 · 2026-09-14 · 커머스 결제 승인 알림

1. **오늘 해결할 문제**: C006 첫 유일 문자와 B006 실제 상품 주문의 결제 승인 알림을 구현한다.
2. **왜 중요한지**: PG가 승인해도 우리 결제가 대기로 남으면 구매자는 결과를 알 수 없다. 반복 알림이 새 행을 만들면 같은 거래를 안정적으로 추적할 수 없다.
3. **핵심 개념**: HTTP 요청과 알림, Reader/Saver 조합, Payment의 상태·승인 ID 규칙, 같은 결제에 대한 반복 처리.
4. **완료 기준**: 선택한 공개 테스트 실행, 스토어에서 승인 대기→완료 확인, 기존 결제의 식별자·승인 ID 보존 설명. 코딩 정확성과 복잡도는 별도로 평가한다.

## 이어가는 시스템

이제 `challenge.commerce`의 **FORM 커머스**가 활성 백엔드다. 코딩 문제와 별도로 이 시스템을 계속 발전시킨다. 상품·옵션·재고·주문은 결제를 실행하기 위해 제공했으며 오늘 사용자가 풀 별도 Order 과제는 아니다. 기존 사용자 `pay`의 정상 승인·응답 유실 직후 조회 정책을 실제 주문 금액에 연결했다. 이전 예약 기반 구현과 테스트는 [legacy](../../legacy/README.md)에 그대로 보존한다.

Day5의 좁은 계약 통과를 결제 시스템 전체 완료로 보지 않는다. 승인 알림과 결제 객체의 상태 변경이 Day6의 새 범위다. 기존 미완성 풀이를 정답으로 대체하지 않았다.

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

## B006 — 기존 결제에 승인 사실 반영하기

스토어에서 상품 옵션을 고르고 주문한다. 결제를 요청했을 때 PG가 처리 중이면 주문 내역에 **승인 확인 중**이 표시된다. PG가 나중에 승인을 완료하고 HTTP 알림을 보내면 같은 결제가 PAID가 되어야 한다. 동일 알림은 여러 번 도착할 수 있다.

### 제공한 시스템과 고정 계약

- 하나의 옵션, 수량 1~5개를 주문한다. 금액은 서버가 상품 가격으로 계산하고 주문에 상품명·옵션·단가·수량·합계를 기록한다. 결제 요청에는 주문 ID만 사용한다.
- 주문 생성 트랜잭션에서 조건부 SQL로 옵션 재고를 차감한다. 재고 부족이면 주문을 생성하지 않는다. 오늘은 취소·재고 반환·배송·장바구니·사용자 인증을 다루지 않는다.
- `UNPAID`는 결제 행이 없는 주문 조회 표현이다. 저장된 결제 상태는 PENDING/PAID다. 주문 조회가 결제 상태를 함께 읽으므로 별도 주문 상태와 결제 상태를 중복 저장하지 않는다.
- PG는 `PaymentGateway` 뒤의 메모리 시뮬레이터다. 실제 돈·외부 결제망은 사용하지 않는다. PG 승인 완료와 우리 DB 반영은 별도 사건이다.
- 오늘 알림 입력은 기존 거래와 같은 주문 ID/key/금액, 유효한 APPROVED 상태·승인 ID다. 순차 호출만 다룬다. 검증·서명·불일치·동시 알림은 오늘 범위 밖이다.
- 로컬 결제가 있으면 **같은 행**을 PAID로 바꾸고 승인 ID를 기록해 `true`를 반환한다. 같은 알림 반복도 `true`, 행 수·식별자·승인 ID는 유지한다.
- 로컬 결제가 없으면 `false`, DB는 바뀌지 않는다. 알림을 새 결제 생성 요청으로 취급하지 않는다.
- 알림 후 결제 재요청은 동일 승인 결과를 반환한다. 알림 없이 PENDING 재요청만으로 사후 승인을 복구하는 기존 `pay` 문제는 후속 범위다.

```text
FE / 외부 PG 요청
        ↓
api/Controller
        ↓
domain/Service            — 유스케이스와 트랜잭션
        ↓
Reader / Saver / Payment  — 조회·저장 조합과 상태 규칙
        ↓
Repository / PaymentGateway 계약
        ↑
infra/db (JPA) / infra/pg (가짜 PG)
```

Controller는 요청 검증·HTTP 응답을 맡는다. 업무 Service는 Reader/Saver를 조합한다. JPA Entity·Spring Data Repository·저장 구현은 `infra/db`에 있고, `Payment`에는 JPA 의존과 public setter가 없다. 결제 조회 부품은 결제 처리와 주문 내역에서 재사용한다. 목록 조회는 결제를 한 번에 읽는다. 개발용 PG 조작 Controller는 시뮬레이터를 직접 제어하는 제공 도구다.

### 직접 구현할 두 곳

- [Payment.confirmApproval](../../src/main/java/challenge/commerce/domain/payment/Payment.java): PENDING→PAID와 승인 ID 기록, 같은 승인 반복의 의미를 구현한다. 현재 TODO 예외를 던진다.
- [PaymentService.onNotification](../../src/main/java/challenge/commerce/domain/payment/PaymentService.java): 기존 사용자 작성본의 새 결제 생성 동작이 남아 있다. PaymentReader·Payment의 업무 동작·PaymentSaver를 조합하여 위 계약을 만족시킨다.

상품/주문 Controller, 조회 Service, Reader/Saver, JPA 어댑터, DB fixture, FE, PG 제어 화면, 테스트와 Spotless는 제공 환경이다. 트랜잭션 소유권은 Service에 두며 테스트용 callback/flush/latch/수동 트랜잭션을 업무 코드에 넣지 않는다.

구현 전 세 가지를 생각한다: **어떤 기존 결제를 읽는가? 상태와 승인 ID의 규칙을 누가 소유하는가? 중복 알림에서 무엇을 보존하는가?** Service에 모든 규칙을 두는 방식과 Reader/Saver·Payment를 조합하는 방식의 이점·비용을 비교한다.

## 실행과 직접 확인

```sh
cd study/challenge-120
./gradlew run
# 스토어: http://127.0.0.1:18086/
# PG 개발 도구: http://127.0.0.1:18086/dev.html
```

1. 개발 도구에서 `처리 중` 모드를 적용한다.
2. 스토어에서 옵션을 골라 주문하고 결제한다. 주문 번호와 **승인 확인 중**을 확인한다.
3. 개발 도구에 주문 번호를 입력하고 **PG 승인 완료**를 누른다. 스토어 새로고침만으로는 여전히 대기다.
4. **같은 승인 알림 전송**을 누른다. 구현 후에는 완료가 표시되어야 한다. 같은 버튼을 다시 눌러도 같은 승인이다.

현재 4번은 409 오류가 나고 PENDING을 유지한다. 이것이 고쳐야 할 과제다. PG 화면은 오류를 성공으로 표시하지 않는다. 정상 승인 모드에서는 지금도 실제 Controller·DB를 거쳐 결제 완료를 확인할 수 있다. 서버를 재시작하면 상품 재고·주문·결제·PG 원장이 초기화된다.

[HTTP 요청 파일](../../requests/payment.http) 또는 `python3 tools/payment-flow.py`로도 같은 흐름을 실행한다. Python 도구는 서버에서 받은 실제 주문 ID와 PG receipt를 사용한다.

```sh
./gradlew commerceInfrastructureTest                  # 제공 환경 9개
./gradlew test --tests 'challenge.commerce.Payment*'  # 오늘 업무 계약 5개
./gradlew test --tests 'challenge.commerce.Payment*' -Pminimum
./gradlew test --tests 'challenge.coding.FirstUniqueTest'
./gradlew test --tests 'challenge.commerce.*' --tests 'challenge.coding.FirstUniqueTest'
./gradlew commerceInfrastructureTest -Pmysql          # Docker의 테스트 전용 MySQL
./gradlew spotlessApply spotlessCheck
./gradlew assemble testClasses legacyTestsClasses    # 과제 미완성과 별도로 컴파일·패키징
```

IntelliJ `Commerce - Server`, `Commerce - Infrastructure`, `Commerce - Payment Day6`, `Commerce - Day6 All` 실행 구성을 제공한다. Spotless는 활성 커머스와 C006 Java를 AOSP 4칸으로 정리한다.

## 시간과 제출

기본 90분 = Coding30(수작업5/기본15/최적화·검증10) + Backend45(화면 관찰5/설계8/구현22/실행·설명10) + 통합복습15.
Minimum45분 = Coding15 + Backend20(관찰3/설계4/구현8/실행·설명5) + 통합복습10. Backend는 상태 규칙 1개·알림 3개를 확인하고 동기 승인 뒤 알림 1개는 `full`로 제외한다. 새로운 업무 규칙이 아니라 같은 상태 규칙을 단위·HTTP 수준에서 확인한다.

환경 대기는 풀이 시간에서 제외한다. Minimum을 전체 완료로 평가하거나 남은 검사를 밀린 숙제로 강제하지 않는다. Phase·Weekly Review·Benchmark·통합복습 원칙은 유지한다. 제출은 초기 접근, 실제 테스트/화면 결과, 사용 시간과 도움, Payment와 Reader/Saver의 책임 설명이다. 사용자 구현·독립 설명·점수는 아직 완료로 기록하지 않는다.

준비 검증: H2/MySQL 제공 환경 각각 9/9, 기존 CI15개 통과. 새 상태 전이 1개·알림 4개는 미완성으로 실패한다. 실제 데스크톱·모바일 브라우저에서 정상 구매, PG 승인 후 알림 실패·대기 유지까지 확인했다.
