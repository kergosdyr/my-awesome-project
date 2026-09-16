package challenge.commerce.domain.catalog;

import challenge.commerce.infra.db.ProductEntity;
import challenge.commerce.infra.db.ProductOptionEntity;
import java.util.List;

public interface ProductRepository {
    List<ProductEntity> findAll();

    List<ProductOptionEntity> findAllOptions();

    boolean takeStock(long optionId, int quantity);
}
