package challenge.commerce.infra.db;

import challenge.commerce.domain.payment.Payment;
import jakarta.persistence.*;

@Entity
@Table(name = "store_payment")
public class PaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(unique = true, nullable = false)
    long orderId;

    @Column(unique = true, nullable = false)
    String paymentKey;

    long amount;

    @Enumerated(EnumType.STRING)
    Payment.Status status;

    String approvalId;

    protected PaymentEntity() {}

    static PaymentEntity from(Payment p) {
        var e = new PaymentEntity();
        e.id = p.id();
        e.orderId = p.orderId();
        e.paymentKey = p.key();
        e.amount = p.amount();
        e.status = p.status();
        e.approvalId = p.approvalId();
        return e;
    }

    Payment toDomain() {
        return new Payment(id, orderId, paymentKey, amount, status, approvalId);
    }

    void reflect(Payment p) {
        status = p.status();
        approvalId = p.approvalId();
    }
}
