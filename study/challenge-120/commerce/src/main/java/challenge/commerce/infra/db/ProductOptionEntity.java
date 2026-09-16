package challenge.commerce.infra.db;

import jakarta.persistence.*;

@Entity
@Table(name = "store_option")
public class ProductOptionEntity {
    @Id
    long id;

    long productId;
    String color;
    String size;
    int stock;

    protected ProductOptionEntity() {}

    public ProductOptionEntity(long id, long productId, String color, String size, int stock) {
        this.id = id;
        this.productId = productId;
        this.color = color;
        this.size = size;
        this.stock = stock;
    }

    public long id() {
        return id;
    }

    public long productId() {
        return productId;
    }

    public String color() {
        return color;
    }

    public String size() {
        return size;
    }

    public int stock() {
        return stock;
    }
}
