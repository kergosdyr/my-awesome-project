package io.github.kergosdyr.commercelab.infra.storage.mysql.eventlab;

import io.github.kergosdyr.commercelab.domain.eventlab.EventLabFixtureRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EventLabFixtureRepositoryImpl implements EventLabFixtureRepository {

    private final JdbcTemplate jdbcTemplate;

    public EventLabFixtureRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void reset() {
        jdbcTemplate.update("delete from processed_order_events");
        jdbcTemplate.update("delete from order_events_outbox");
        jdbcTemplate.update("delete from order_lines");
        jdbcTemplate.update("delete from orders");
        jdbcTemplate.update("""
                update products
                set stock_quantity = case id
                    when 1 then 120
                    when 2 then 80
                    when 3 then 55
                    when 4 then 42
                    when 5 then 18
                    when 6 then 0
                end,
                status = case when id = 6 then 'SOLD_OUT' else 'ACTIVE' end
                where id between 1 and 6
                """);
    }
}
