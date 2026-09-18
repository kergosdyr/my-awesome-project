package challenge.commerce.infra.db;

import challenge.commerce.domain.catalog.ProductRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepositoryImpl implements ProductRepository {
    private final ProductJpaRepository productJpaRepository;
    private final ProductOptionJpaRepository productOptionJpaRepository;

    public ProductRepositoryImpl(
            ProductJpaRepository productJpaRepository, ProductOptionJpaRepository productOptionJpaRepository) {
        this.productJpaRepository = productJpaRepository;
        this.productOptionJpaRepository = productOptionJpaRepository;
    }

    @Override
    public List<ProductEntity> findAll() {
        return productJpaRepository.findAll(Sort.by("id"));
    }

    @Override
    public List<ProductOptionEntity> findAllOptions() {
        return productOptionJpaRepository.findAll(Sort.by("id"));
    }

    @Override
    public List<ProductEntity> findByIds(List<Long> ids) {
        return productJpaRepository.findAllById(ids);
    }

    @Override
    public List<ProductOptionEntity> findOptionsByIds(List<Long> ids) {
        return productOptionJpaRepository.findAllById(ids);
    }

    @Override
    public boolean takeStock(long id, int quantity) {
        return productOptionJpaRepository.takeStock(id, quantity) == 1;
    }
}
