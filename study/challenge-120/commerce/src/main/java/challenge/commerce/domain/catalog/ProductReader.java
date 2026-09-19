package challenge.commerce.domain.catalog;

import challenge.commerce.domain.query.PageQuery;
import challenge.commerce.support.BusinessException;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class ProductReader {
    private final ProductRepository productRepository;

    public ProductReader(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResult> readPage(PageQuery query) {
        return productRepository.findPage(query);
    }

    public Map<Long, ProductSelectionResult> readForOptions(List<Long> optionIds) {
        var selections = productRepository.findSelections(optionIds);
        if (selections.size() != optionIds.size()) {
            throw BusinessException.notFound("상품 또는 옵션을 찾을 수 없습니다.");
        }
        return selections;
    }
}
