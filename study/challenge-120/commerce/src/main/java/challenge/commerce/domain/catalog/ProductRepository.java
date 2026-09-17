package challenge.commerce.domain.catalog;

import challenge.commerce.infra.db.ProductEntity;
import challenge.commerce.infra.db.ProductOptionEntity;
import java.util.List;

public interface ProductRepository {
    List<ProductEntity> findAll();

    List<ProductOptionEntity> findAllOptions();

    List<ProductEntity> findByIds(List<Long> ids);

    List<ProductOptionEntity> findOptionsByIds(List<Long> ids);

    boolean takeStock(long optionId, int quantity);
}
