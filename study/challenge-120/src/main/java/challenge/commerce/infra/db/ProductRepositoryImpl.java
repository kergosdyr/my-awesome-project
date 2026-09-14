package challenge.commerce.infra.db;

import challenge.commerce.domain.catalog.Product;
import challenge.commerce.domain.catalog.ProductRepository;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepositoryImpl implements ProductRepository {
    private final ProductJpaRepository products;
    private final ProductOptionJpaRepository options;

    public ProductRepositoryImpl(
            ProductJpaRepository products, ProductOptionJpaRepository options) {
        this.products = products;
        this.options = options;
    }

    @Override
    public List<Product> findAll() {
        var allOptions = options.findAll(Sort.by("id"));
        return products.findAll(Sort.by("id")).stream()
                .map(
                        p ->
                                new Product(
                                        p.id,
                                        p.brand,
                                        p.name,
                                        p.description,
                                        p.category,
                                        p.price,
                                        p.image,
                                        allOptions.stream()
                                                .filter(o -> o.productId == p.id)
                                                .map(
                                                        o ->
                                                                new Product.Option(
                                                                        o.id, o.color, o.size,
                                                                        o.stock))
                                                .toList()))
                .toList();
    }

    @Override
    public Optional<Product> findByOptionId(long id) {
        // Three-product fixture: reuse the catalog read model; optimize when the catalog scope
        // grows.
        return findAll().stream()
                .filter(p -> p.options().stream().anyMatch(o -> o.id() == id))
                .findFirst();
    }

    @Override
    public boolean takeStock(long id, int quantity) {
        return options.takeStock(id, quantity) == 1;
    }
}
