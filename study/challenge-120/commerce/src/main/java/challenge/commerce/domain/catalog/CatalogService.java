package challenge.commerce.domain.catalog;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
    private final ProductReader productReader;

    public CatalogService(ProductReader productReader) {
        this.productReader = productReader;
    }

    @Transactional(readOnly = true)
    public List<ProductResult> list() {
        return productReader.readAll();
    }
}
