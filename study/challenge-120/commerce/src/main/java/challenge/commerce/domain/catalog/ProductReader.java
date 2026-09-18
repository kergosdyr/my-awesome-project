package challenge.commerce.domain.catalog;

import challenge.commerce.infra.db.ProductEntity;
import challenge.commerce.infra.db.ProductOptionEntity;
import challenge.commerce.support.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class ProductReader {
    private final ProductRepository productRepository;

    public ProductReader(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResult> readAll() {
        var optionsByProduct = productRepository.findAllOptions().stream()
                .collect(Collectors.groupingBy(ProductOptionEntity::productId));
        return productRepository.findAll().stream()
                .map(product -> new ProductResult(product, optionsByProduct.getOrDefault(product.id(), List.of())))
                .toList();
    }

    public Map<Long, ProductSelectionResult> readForOptions(List<Long> optionIds) {
        var options = productRepository.findOptionsByIds(optionIds);
        if (options.size() != optionIds.size()) {
            throw BusinessException.notFound("상품 옵션을 찾을 수 없습니다.");
        }
        var productById =
                productRepository
                        .findByIds(options.stream()
                                .map(ProductOptionEntity::productId)
                                .distinct()
                                .toList())
                        .stream()
                        .collect(Collectors.toMap(ProductEntity::id, Function.identity()));
        return options.stream().collect(Collectors.toMap(ProductOptionEntity::id, option -> {
            var product = productById.get(option.productId());
            if (product == null) throw BusinessException.notFound("상품을 찾을 수 없습니다.");
            return new ProductSelectionResult(product, option);
        }));
    }
}
