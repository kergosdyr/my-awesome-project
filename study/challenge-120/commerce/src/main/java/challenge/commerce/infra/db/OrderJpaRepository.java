package challenge.commerce.infra.db;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {
    @Query("select distinct o from OrderEntity o left join fetch o.items where o.id = :id")
    Optional<OrderEntity> findWithItemsById(@Param("id") long id);

    @Query("select distinct o from OrderEntity o left join fetch o.items order by o.id desc")
    List<OrderEntity> findAllWithItems();
}
