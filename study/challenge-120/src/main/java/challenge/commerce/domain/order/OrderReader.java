package challenge.commerce.domain.order;

import challenge.commerce.support.BusinessException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class OrderReader {
    private final OrderRepository orders;

    public OrderReader(OrderRepository orders) {
        this.orders = orders;
    }

    public PurchaseOrder read(long id) {
        return orders.findById(id).orElseThrow(() -> BusinessException.notFound("주문을 찾을 수 없습니다."));
    }

    public List<PurchaseOrder> readAll() {
        return orders.findAll();
    }
}
