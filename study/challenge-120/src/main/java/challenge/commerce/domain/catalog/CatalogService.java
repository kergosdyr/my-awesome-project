package challenge.commerce.domain.catalog;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
    private final ProductReader products;

    public CatalogService(ProductReader products) {
        this.products = products;
    }

    @Transactional(readOnly = true)
    public List<Product> list() {
        return products.readAll();
    }
}
