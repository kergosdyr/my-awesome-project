package challenge.commerce.domain.payment;

/**
 * B006: PG 승인 사실을 가진 결제 객체. JPA 의존 없이 상태 규칙을 소유한다. 오늘 해결할 문제: PENDING 결제에 승인 알림이 도착하면 상태와 승인 ID를 함께
 * 확정한다. 왜 중요한가: PAID인데 승인 ID가 없거나 반복 알림이 새 결제를 만들면 주문 결과를 신뢰할 수 없다. 핵심 개념: 결제 상태 전이, 같은 결제 식별, 반복
 * 처리. 완료 기준: confirmApproval과 PaymentService.onNotification 계약 및 실제 화면 확인. 도메인 상태에는 응답 전용
 * NOT_FOUND/CONFLICT를 저장하지 않는다.
 */
public class Payment {
    public enum Status {
        PENDING,
        PAID
    }

    private final Long id;
    private final long orderId;
    private final String key;
    private final long amount;
    private Status status;
    private String approvalId;

    public Payment(Long id, long orderId, String key, long amount, Status status, String approvalId) {
        this.id = id;
        this.orderId = orderId;
        this.key = key;
        this.amount = amount;
        this.status = status;
        this.approvalId = approvalId;
    }

    /** TODO B006: 같은 승인 ID 반복을 허용하고 상태·승인 ID를 함께 변경한다. 입력은 유효한 동일 승인 ID다. */
    public void confirmApproval(String approvalId) {
        this.status = Status.PAID;
        this.approvalId = approvalId;
    }

    public Long id() {
        return id;
    }

    public long orderId() {
        return orderId;
    }

    public String key() {
        return key;
    }

    public long amount() {
        return amount;
    }

    public Status status() {
        return status;
    }

    public String approvalId() {
        return approvalId;
    }

    public boolean isPaid() {
        return status == Status.PAID;
    }
}
