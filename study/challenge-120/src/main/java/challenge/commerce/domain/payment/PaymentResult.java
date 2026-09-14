package challenge.commerce.domain.payment;

public record PaymentResult(Status status, String approvalId) {
    /**
     * PENDING=로컬 승인 미확정(처리중 또는 응답 유실), PAID=로컬 승인 확인 완료. NOT_FOUND/CONFLICT는 응답 전용이며 결제 진행 상태가 아니다.
     */
    public enum Status {
        PENDING,
        PAID,
        NOT_FOUND,
        CONFLICT
    }
}
