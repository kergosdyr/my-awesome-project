package challenge.commerce.domain.catalog;

import challenge.commerce.infra.db.ProductEntity;
import challenge.commerce.infra.db.ProductOptionEntity;

/** 주문에서 선택한 상품과 옵션. */
public record ProductSelectionResult(ProductEntity product, ProductOptionEntity option) {}
