package challenge.commerce.domain.order;

import challenge.commerce.domain.payment.PaymentReader;
import challenge.commerce.infra.db.OrderEntity;
import challenge.commerce.infra.db.PaymentEntity;
import java.util.List;
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
    public OrderDetailsResult find(long id) {
        var order = orders.read(id);
        return payments.readByOrderId(id)
                .map(payment -> OrderDetailsResult.withPayment(order, payment))
                .orElseGet(() -> OrderDetailsResult.withoutPayment(order));
    }

    @Transactional(readOnly = true)
    public List<OrderDetailsResult> list() {
        var allOrders = orders.readAll();
        var paymentByOrder = payments
                .readForOrders(allOrders.stream().map(OrderEntity::id).toList())
                .stream()
                .collect(Collectors.toMap(PaymentEntity::orderId, Function.identity()));
        return allOrders.stream()
                .map(order -> {
                    var payment = paymentByOrder.get(order.id());
                    return payment == null
                            ? OrderDetailsResult.withoutPayment(order)
                            : OrderDetailsResult.withPayment(order, payment);
                })
                .toList();
    }
}
