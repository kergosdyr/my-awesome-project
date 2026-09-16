package challenge.commerce.api.response;

import challenge.commerce.domain.catalog.ProductResult;
import challenge.commerce.infra.db.ProductOptionEntity;
import java.util.List;

public record ProductResponse(
        long id,
        String brand,
        String name,
        String description,
        String category,
        long price,
        String image,
        List<OptionResponse> options) {
    public static ProductResponse from(ProductResult result) {
        var product = result.product();
        return new ProductResponse(
                product.id(),
                product.brand(),
                product.name(),
                product.description(),
                product.category(),
                product.price(),
                product.image(),
                result.options().stream().map(OptionResponse::from).toList());
    }

    public record OptionResponse(long id, String color, String size, int stock) {
        public static OptionResponse from(ProductOptionEntity option) {
            return new OptionResponse(option.id(), option.color(), option.size(), option.stock());
        }
    }
}
