package challenge.commerce.domain.order;

import challenge.commerce.domain.payment.Payment;
import challenge.commerce.domain.payment.PaymentReader;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderQueryService {
    private final OrderReader orders;
    private final PaymentReader payments;

    public OrderQueryService(OrderReader orders, PaymentReader payments) {
        this.orders = orders;
        this.payments = payments;
    }

    @Transactional(readOnly = true)
    public OrderDetails find(long id) {
        return details(orders.read(id), payments.readByOrderId(id));
    }

    private OrderDetails details(PurchaseOrder order, Optional<Payment> payment) {
        return new OrderDetails(
                order,
                payment.map(p -> p.status().name()).orElse("UNPAID"),
                payment.map(Payment::approvalId).orElse(null));
    }

    @Transactional(readOnly = true)
    public List<OrderDetails> list() {
        var allOrders = orders.readAll();
        var paymentByOrder =
                payments.readForOrders(allOrders.stream().map(PurchaseOrder::id).toList()).stream()
                        .collect(Collectors.toMap(Payment::orderId, Function.identity()));
        return allOrders.stream()
                .map(order -> details(order, Optional.ofNullable(paymentByOrder.get(order.id()))))
                .toList();
    }

    public record OrderDetails(PurchaseOrder order, String paymentStatus, String approvalId) {}
}
