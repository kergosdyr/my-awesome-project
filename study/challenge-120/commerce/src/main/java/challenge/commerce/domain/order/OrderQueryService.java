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

    /**
     * B009: 최근 주문부터 size개와 다음 요청 표식을 반환한다.
     * after=null은 첫 요청. createdAt 내림차순, 같은 시각은 id 내림차순.
     * 새 최신 주문이 들어와도 기존 주문을 중복·누락 없이 이어 읽는다.
     * 쿼리·건수 제한은 infra에, 결제 조합과 트랜잭션은 이 서비스에 둔다.
     * 실행: ./gradlew :commerce:test --tests '*OrderWindowTest'
     */
    @Transactional(readOnly = true)
    public OrderWindowResult window(int size, String after) {
        throw new UnsupportedOperationException("B009: 주문 페이지 조회를 구현하세요.");
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
