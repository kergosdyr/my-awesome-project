package challenge.payment;

import jakarta.persistence.*;

@Entity
@Table(name = "payment")
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private Long reservationId;
    @Column(nullable = false, unique = true)
    private String paymentKey;
    @Column(nullable = false)
    private long amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PaymentResult.Status status;
    private String approvalId;
    protected Payment() {}
    public Payment(Long reservationId, String paymentKey, long amount, PaymentResult.Status status) {
        this.reservationId = reservationId; this.paymentKey = paymentKey; this.amount = amount; this.status = status;
    }
    public Long getId() { return id; }
    public Long getReservationId() { return reservationId; }
    public String getPaymentKey() { return paymentKey; }
    public long getAmount() { return amount; }
    public PaymentResult.Status getStatus() { return status; }
    public String getApprovalId() { return approvalId; }
    public void setStatus(PaymentResult.Status status) { this.status = status; }
    public void setApprovalId(String approvalId) { this.approvalId = approvalId; }
}
