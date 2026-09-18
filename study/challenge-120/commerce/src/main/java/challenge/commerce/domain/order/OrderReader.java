package challenge.commerce.domain.order;

import challenge.commerce.domain.query.PageQuery;
import challenge.commerce.infra.db.OrderEntity;
import challenge.commerce.support.BusinessException;
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

    public OrderDetailsResult readDetails(long id) {
        return orderRepository.findDetailsById(id).orElseThrow(() -> BusinessException.notFound("주문을 찾을 수 없습니다."));
    }

    public OrderPageResult readPage(PageQuery query) {
        return orderRepository.findPage(query);
    }

    public OrderWindowResult readCursor(int size, OrderCursor cursor) {
        return orderRepository.findCursor(size, cursor);
    }
}
