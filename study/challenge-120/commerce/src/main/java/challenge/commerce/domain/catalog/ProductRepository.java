package challenge.commerce.domain.catalog;

import challenge.commerce.domain.query.PageQuery;
import java.util.List;
import java.util.Map;

public interface ProductRepository {
    List<ProductResult> findPage(PageQuery query);

    Map<Long, ProductSelectionResult> findSelections(List<Long> optionIds);

    boolean takeStock(long optionId, int quantity);
}
