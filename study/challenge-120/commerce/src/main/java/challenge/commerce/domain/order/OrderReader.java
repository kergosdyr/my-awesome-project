package challenge.commerce.domain.order;

import challenge.commerce.infra.db.OrderEntity;
import challenge.commerce.support.BusinessException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class OrderReader {
    private final OrderRepository orderRepository;

    public OrderReader(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public OrderEntity read(long id) {
        return orderRepository.findById(id).orElseThrow(() -> BusinessException.notFound("주문을 찾을 수 없습니다."));
    }

    public List<OrderEntity> readAll() {
        return orderRepository.findAll();
    }
}
