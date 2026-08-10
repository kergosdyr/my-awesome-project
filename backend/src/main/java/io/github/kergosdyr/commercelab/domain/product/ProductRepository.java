package io.github.kergosdyr.commercelab.domain.product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    List<ProductEntity> readAll();

    Optional<ProductEntity> readById(Long productId);

    List<ProductEntity> readAllByIdForUpdate(List<Long> productIds);
}
