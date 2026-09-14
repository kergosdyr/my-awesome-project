package challenge.payment;

import jakarta.persistence.*;

/**
 * 결제 상태를 소유하는 객체. Day6 설계 TODO: 승인 확인 시 상태·승인ID를 함께 다루는 동작을 설계하고 onNotification에서 사용한다. 같은 승인 알림이
 * 다시 와도 기존 결제 ID·승인ID는 유지해야 한다. 현재 생성자/setter와 PaymentResult.Status 결합은 사용자 작성본으로 보존했다. 상태 전용 타입
 * 분리, 유효하지 않은 PAID 생성 방지와 setter 축소를 기존 시간 안에서 검토한다. 별도 Entity/Domain 복제나 새 계층을 의무로 만들지 않는다.
 */
@Entity
@Table(name = "payment")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long reservationId;

    @Column(nullable = false, unique = true)
    private String paymentKey;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentResult.Status status;

    private String approvalId;

    protected Payment() {}

    public Payment(
            Long reservationId, String paymentKey, long amount, PaymentResult.Status status) {
        this.reservationId = reservationId;
        this.paymentKey = paymentKey;
        this.amount = amount;
        this.status = status;
    }

    public Payment(
            Long reservationId,
            String paymentKey,
            String approvalId,
            long amount,
            PaymentResult.Status status) {
        this.reservationId = reservationId;
        this.paymentKey = paymentKey;
        this.approvalId = approvalId;
        this.amount = amount;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public String getPaymentKey() {
        return paymentKey;
    }

    public long getAmount() {
        return amount;
    }

    public PaymentResult.Status getStatus() {
        return status;
    }

    public String getApprovalId() {
        return approvalId;
    }

    public void setStatus(PaymentResult.Status status) {
        this.status = status;
    }

    public void setApprovalId(String approvalId) {
        this.approvalId = approvalId;
    }

    public boolean isPaid() {
        return this.status == PaymentResult.Status.PAID;
    }
}
