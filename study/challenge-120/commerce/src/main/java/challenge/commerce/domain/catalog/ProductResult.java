package challenge.commerce.domain.catalog;

import challenge.commerce.infra.db.ProductEntity;
import challenge.commerce.infra.db.ProductOptionEntity;
import java.util.List;

/** 상품과 옵션을 함께 조회한 결과. Entity 필드를 복제하지 않는다. */
public record ProductResult(ProductEntity product, List<ProductOptionEntity> options) {}
