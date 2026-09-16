package challenge.commerce.infra.db;

import challenge.commerce.domain.catalog.ProductRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepositoryImpl implements ProductRepository {
    private final ProductJpaRepository products;
    private final ProductOptionJpaRepository options;

    public ProductRepositoryImpl(ProductJpaRepository products, ProductOptionJpaRepository options) {
        this.products = products;
        this.options = options;
    }

    @Override
    public List<ProductEntity> findAll() {
        return products.findAll(Sort.by("id"));
    }

    @Override
    public List<ProductOptionEntity> findAllOptions() {
        return options.findAll(Sort.by("id"));
    }

    @Override
    public boolean takeStock(long id, int quantity) {
        return options.takeStock(id, quantity) == 1;
    }
}
