package challenge.commerce.infra.db;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProductOptionJpaRepository extends JpaRepository<ProductOptionEntity, Long> {
    @Modifying
    @Query("update ProductOptionEntity o set o.stock = o.stock - :quantity where o.id = :id and o.stock >= :quantity")
    int takeStock(@Param("id") long id, @Param("quantity") int quantity);
}
