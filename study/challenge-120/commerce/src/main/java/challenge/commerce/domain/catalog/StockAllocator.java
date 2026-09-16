package challenge.commerce.domain.catalog;

import challenge.commerce.support.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 제공 주문 인프라. 주문 생성과 같은 DB 트랜잭션에서 수량을 확보한다. */
@Component
@Transactional
public class StockAllocator {
    private final ProductRepository products;

    public StockAllocator(ProductRepository products) {
        this.products = products;
    }

    public void allocate(long optionId, int quantity) {
        if (!products.takeStock(optionId, quantity)) throw BusinessException.conflict("선택한 옵션의 재고가 부족합니다.");
    }
}
