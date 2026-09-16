package challenge.commerce.domain.catalog;

import challenge.commerce.infra.db.ProductOptionEntity;
import challenge.commerce.support.BusinessException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class ProductReader {
    private final ProductRepository products;

    public ProductReader(ProductRepository products) {
        this.products = products;
    }

    public List<ProductResult> readAll() {
        var optionsByProduct =
                products.findAllOptions().stream().collect(Collectors.groupingBy(ProductOptionEntity::productId));
        return products.findAll().stream()
                .map(product -> new ProductResult(product, optionsByProduct.getOrDefault(product.id(), List.of())))
                .toList();
    }

    public ProductResult readForOption(long optionId) {
        return readAll().stream()
                .filter(product -> product.options().stream().anyMatch(option -> option.id() == optionId))
                .findFirst()
                .orElseThrow(() -> BusinessException.notFound("상품 옵션을 찾을 수 없습니다."));
    }
}
