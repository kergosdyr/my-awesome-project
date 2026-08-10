package io.github.kergosdyr.commercelab.infra.storage.mysql.product;

import java.util.List;

import io.github.kergosdyr.commercelab.domain.product.ProductEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {

    @Query("select product from ProductEntity product order by product.id")
    List<ProductEntity> findAllInCatalogOrder();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select product from ProductEntity product where product.id in :productIds order by product.id")
    List<ProductEntity> findAllByIdForUpdate(@Param("productIds") List<Long> productIds);
}
