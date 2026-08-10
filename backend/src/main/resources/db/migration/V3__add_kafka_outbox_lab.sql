CREATE TABLE order_events_outbox (
    event_id VARCHAR(36) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    topic VARCHAR(200) NOT NULL,
    event_key VARCHAR(100) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    payload TEXT NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    published_at TIMESTAMP(6) NULL,
    publish_attempts INT NOT NULL DEFAULT 0,
    publish_failures INT NOT NULL DEFAULT 0,
    last_error VARCHAR(500) NULL,
    PRIMARY KEY (event_id),
    CONSTRAINT fk_order_events_outbox_order FOREIGN KEY (aggregate_id) REFERENCES orders (id)
);

CREATE INDEX idx_order_events_outbox_pending
    ON order_events_outbox (published_at, occurred_at);

CREATE TABLE processed_order_events (
    event_id VARCHAR(36) NOT NULL,
    order_id BIGINT NOT NULL,
    order_number VARCHAR(64) NOT NULL,
    processed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (event_id)
);

CREATE INDEX idx_processed_order_events_order_id
    ON processed_order_events (order_id);
