# B006 — 결제 승인 알림

발행: Day6 · 2026-09-14. 사용자 구현·공개5/5 확인. 코딩은 [C006](C006-FirstUnique.md)에서 별도로 찾는다.

## 상황과 완료 기준

고객이 결제를 눌렀지만 승인 대기로 표시된다. 개발용 PG에서 승인을 완료하고 알림을 보내면 기존 결제 한 건의 상태와 승인 번호가 바뀌어야 한다. 같은 알림을 다시 보내도 새 결제를 만들지 않는다. 우리 쪽 결제 기록이 없으면 false를 반환하며 DB를 변경하지 않는다.

- [PaymentEntity.confirmApproval](../../commerce/src/main/java/challenge/commerce/infra/db/PaymentEntity.java): 사용자 상태 변경 구현을 Entity로 옮겨 보존했다.
- [PaymentService.onNotification](../../commerce/src/main/java/challenge/commerce/domain/payment/PaymentService.java): 기존 결제를 조회해 변경하며 서비스 트랜잭션이 DB에 반영한다.
- [공개 테스트](../../commerce/src/test/java/challenge/commerce/PaymentNotificationContractTest.java): 실제 HTTP 알림·반복 처리·없는 결제를 검사한다.

```sh
./gradlew :commerce:run
./gradlew :commerce:test --tests 'challenge.commerce.Payment*'
python3 tools/payment-flow.py
```

스토어는 http://127.0.0.1:18086/, 개발용 PG는 http://127.0.0.1:18086/dev.html 에 있다. 테스트는 서버를 직접 띄우므로 별도 기동이 필요 없다. 전체 화면 실험 절차는 [HTTP 요청 파일](../../requests/payment.http)을 따른다.

유효한 동일 주문·승인 번호의 순차 알림을 가정한다. 서명 검증·실제 PG·다른 금액·서로 다른 승인 번호 처리는 발행 당시 완료 범위가 아니다. 통과를 독립 설계 설명·지연 회상 성공으로 대신하지 않는다.
