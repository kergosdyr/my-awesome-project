package challenge.commerce.domain.order;

import java.util.List;

public record OrderPageResult(List<OrderDetailsResult> entries, int page, int size, boolean hasNext) {}
