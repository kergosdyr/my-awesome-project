package io.github.kergosdyr.commercelab.infra.storage.mysql.order;

import io.github.kergosdyr.commercelab.domain.order.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {
}
