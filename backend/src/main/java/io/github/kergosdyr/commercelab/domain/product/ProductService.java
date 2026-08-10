package io.github.kergosdyr.commercelab.domain.product;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductReader productReader;

    public ProductService(ProductReader productReader) {
        this.productReader = productReader;
    }

    @Transactional(readOnly = true)
    public List<ProductEntity> listProducts(ProductListQuery query) {
        return productReader.readProducts(query);
    }

    @Transactional(readOnly = true)
    public ProductEntity getProduct(Long productId) {
        return productReader.readProduct(productId);
    }
}
