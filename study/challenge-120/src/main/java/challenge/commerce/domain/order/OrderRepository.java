package challenge.commerce.domain.order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    PurchaseOrder create(PurchaseOrder order);

    Optional<PurchaseOrder> findById(long id);

    List<PurchaseOrder> findAll();
}
