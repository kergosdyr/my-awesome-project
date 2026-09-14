package challenge.commerce.domain.catalog;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    List<Product> findAll();

    Optional<Product> findByOptionId(long optionId);

    boolean takeStock(long optionId, int quantity);
}
