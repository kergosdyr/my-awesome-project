package io.github.kergosdyr.commercelab.infra.storage.mysql.eventlab;

import java.time.Instant;

import io.github.kergosdyr.commercelab.domain.eventlab.OrderPlacedEvent;
import io.github.kergosdyr.commercelab.domain.eventlab.ProcessedOrderEventRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProcessedOrderEventRepositoryImpl implements ProcessedOrderEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProcessedOrderEventRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean recordIfFirst(OrderPlacedEvent event, Instant processedAt) {
        var inserted = jdbcTemplate.update(
                "insert ignore into processed_order_events "
                        + "(event_id, order_id, order_number, processed_at) values (?, ?, ?, ?)",
                event.eventId().toString(),
                event.orderId(),
                event.orderNumber(),
                processedAt
        );
        return inserted == 1;
    }

    @Override
    public long countProcessed() {
        var count = jdbcTemplate.queryForObject("select count(*) from processed_order_events", Long.class);
        return count == null ? 0 : count;
    }
}
