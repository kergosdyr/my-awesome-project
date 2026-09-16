package challenge.commerce.domain.catalog;

import challenge.commerce.support.BusinessException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class ProductReader {
    private final ProductRepository products;

    public ProductReader(ProductRepository products) {
        this.products = products;
    }

    public List<Product> readAll() {
        return products.findAll();
    }

    public Product readForOption(long optionId) {
        return products.findByOptionId(optionId).orElseThrow(() -> BusinessException.notFound("상품 옵션을 찾을 수 없습니다."));
    }
}
