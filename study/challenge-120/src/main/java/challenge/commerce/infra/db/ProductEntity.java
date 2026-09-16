package challenge.commerce.infra.db;

import jakarta.persistence.*;

@Entity
@Table(name = "store_product")
public class ProductEntity {
    @Id
    long id;

    String brand;
    String name;

    @Column(length = 1000)
    String description;

    String category;
    long price;
    String image;

    protected ProductEntity() {}

    public ProductEntity(
            long id, String brand, String name, String description, String category, long price, String image) {
        this.id = id;
        this.brand = brand;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.image = image;
    }
}
