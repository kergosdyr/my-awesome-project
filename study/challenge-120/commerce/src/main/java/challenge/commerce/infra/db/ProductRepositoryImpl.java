package challenge.commerce.infra.db;

import static challenge.commerce.infra.db.QProductEntity.productEntity;
import static challenge.commerce.infra.db.QProductOptionEntity.productOptionEntity;

import challenge.commerce.domain.catalog.*;
import challenge.commerce.domain.query.PageQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepositoryImpl implements ProductRepository {
    private final JPAQueryFactory jpaQueryFactory;

    public ProductRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        this.jpaQueryFactory = jpaQueryFactory;
    }

    @Override
    public List<ProductResult> findPage(PageQuery query) {
        var pageable = PageRequest.of(query.page(), query.size());
        var ids = jpaQueryFactory
                .select(productEntity.id)
                .from(productEntity)
                .orderBy(productEntity.id.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        if (ids.isEmpty()) return List.of();
        var rows = jpaQueryFactory
                .select(productEntity, productOptionEntity)
                .from(productEntity)
                .leftJoin(productOptionEntity)
                .on(productOptionEntity.productId.eq(productEntity.id))
                .where(productEntity.id.in(ids))
                .orderBy(productEntity.id.asc(), productOptionEntity.id.asc())
                .fetch();
        // 조인된 행을 API가 요구하는 상품별 옵션 목록으로 묶는다. Entity 복제는 하지 않는다.
        var optionsByProduct = new LinkedHashMap<ProductEntity, List<ProductOptionEntity>>();
        for (var row : rows) {
            var product = row.get(productEntity);
            var options = optionsByProduct.computeIfAbsent(product, ignored -> new ArrayList<>());
            var option = row.get(productOptionEntity);
            if (option != null) options.add(option);
        }
        return optionsByProduct.entrySet().stream()
                .map(entry -> new ProductResult(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();
    }

    @Override
    public Map<Long, ProductSelectionResult> findSelections(List<Long> optionIds) {
        if (optionIds.isEmpty()) return Map.of();
        return jpaQueryFactory
                .select(productEntity, productOptionEntity)
                .from(productOptionEntity)
                .join(productEntity)
                .on(productOptionEntity.productId.eq(productEntity.id))
                .where(productOptionEntity.id.in(optionIds))
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        row -> row.get(productOptionEntity).id(),
                        row -> new ProductSelectionResult(row.get(productEntity), row.get(productOptionEntity))));
    }

    @Override
    public boolean takeStock(long id, int quantity) {
        return jpaQueryFactory
                        .update(productOptionEntity)
                        .set(productOptionEntity.stock, productOptionEntity.stock.subtract(quantity))
                        .where(productOptionEntity.id.eq(id), productOptionEntity.stock.goe(quantity))
                        .execute()
                == 1;
    }
}
