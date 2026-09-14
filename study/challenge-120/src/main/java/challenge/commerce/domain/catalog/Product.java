package challenge.commerce.domain.catalog;

import java.util.List;

public record Product(
        long id,
        String brand,
        String name,
        String description,
        String category,
        long price,
        String image,
        List<Option> options) {
    public record Option(long id, String color, String size, int stock) {}
}
