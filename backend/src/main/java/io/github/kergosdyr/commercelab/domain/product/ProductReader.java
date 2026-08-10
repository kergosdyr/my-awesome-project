package io.github.kergosdyr.commercelab.domain.product;

import java.util.List;

import io.github.kergosdyr.commercelab.support.error.ApiException;
import io.github.kergosdyr.commercelab.support.error.ErrorType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProductReader {

    private final ProductRepository productRepository;

    public ProductReader(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductEntity> readProducts(ProductListQuery query) {
        return productRepository.readAll();
    }

    @Transactional(readOnly = true)
    public ProductEntity readProduct(Long productId) {
        return productRepository.readById(productId)
                .orElseThrow(() -> new ApiException(ErrorType.PRODUCT_NOT_FOUND));
    }

    @Transactional
    public List<ProductEntity> readProductsForUpdate(List<Long> productIds) {
        return productRepository.readAllByIdForUpdate(productIds);
    }
}
