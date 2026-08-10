package io.github.kergosdyr.commercelab.infra.storage.mysql.product;

import java.util.List;
import java.util.Optional;

import io.github.kergosdyr.commercelab.domain.product.ProductEntity;
import io.github.kergosdyr.commercelab.domain.product.ProductRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    public ProductRepositoryImpl(ProductJpaRepository productJpaRepository) {
        this.productJpaRepository = productJpaRepository;
    }

    @Override
    public List<ProductEntity> readAll() {
        return productJpaRepository.findAllInCatalogOrder();
    }

    @Override
    public Optional<ProductEntity> readById(Long productId) {
        return productJpaRepository.findById(productId);
    }

    @Override
    public List<ProductEntity> readAllByIdForUpdate(List<Long> productIds) {
        return productJpaRepository.findAllByIdForUpdate(productIds);
    }
}
