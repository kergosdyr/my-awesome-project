package challenge.commerce.infra.db;

import challenge.commerce.support.BusinessException;
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

    public long id() {
        return id;
    }

    public String brand() {
        return brand;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public String category() {
        return category;
    }

    public long price() {
        return price;
    }

    public String image() {
        return image;
    }

    public void validatePrice(long displayedPrice) {
        if (this.price != displayedPrice) {
            throw BusinessException.conflict("주문이 실패하였습니다. 가격을 재확인 후 진행바랍니다");
        }
    }
}
