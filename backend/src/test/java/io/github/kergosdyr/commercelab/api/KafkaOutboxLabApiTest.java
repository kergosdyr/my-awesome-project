package io.github.kergosdyr.commercelab.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import io.github.kergosdyr.commercelab.domain.eventlab.OrderEventLabService;
import io.github.kergosdyr.commercelab.domain.eventlab.OrderEventProcessingService;
import io.github.kergosdyr.commercelab.domain.eventlab.OrderEventPublisher;
import io.github.kergosdyr.commercelab.domain.eventlab.OrderPlacedEvent;
import io.github.kergosdyr.commercelab.support.error.ApiException;
import io.github.kergosdyr.commercelab.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KafkaOutboxLabApiTest {

    private static final String ORDER_BODY = """
            {
              "customerName": "Kafka lab",
              "lines": [{"productId": 1, "quantity": 1}]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrderEventLabService eventLabService;

    @Autowired
    private OrderEventProcessingService processingService;

    @MockitoBean
    private OrderEventPublisher eventPublisher;

    @BeforeEach
    void resetLab() {
        reset(eventPublisher);
        eventLabService.resetFixtures();
    }

    @AfterEach
    void restoreSharedTestFixture() {
        eventLabService.resetFixtures();
    }

    @Test
    void writesOrderAndOutboxAtomicallyForOutboxStrategy() throws Exception {
        mockMvc.perform(post("/api/labs/events/orders")
                        .queryParam("strategy", "outbox")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.strategy").value("OUTBOX"))
                .andExpect(jsonPath("$.data.eventId").isNotEmpty());

        org.assertj.core.api.Assertions.assertThat(count("orders")).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(count("order_events_outbox")).isEqualTo(1);

        mockMvc.perform(get("/api/labs/events/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.created").value(1))
                .andExpect(jsonPath("$.data.published").value(0))
                .andExpect(jsonPath("$.data.pending").value(1));
    }

    @Test
    void directStrategyWaitsForPublisherAndKeepsOutboxEmpty() throws Exception {
        mockMvc.perform(post("/api/labs/events/orders")
                        .queryParam("strategy", "direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.strategy").value("DIRECT"));

        verify(eventPublisher).publish(any(OrderPlacedEvent.class));
        org.assertj.core.api.Assertions.assertThat(count("orders")).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(count("order_events_outbox")).isZero();
    }

    @Test
    void directPublishFailureRollsBackOrderAndStock() throws Exception {
        doThrow(new ApiException(ErrorType.EVENT_PUBLISH_FAILED))
                .when(eventPublisher).publish(any(OrderPlacedEvent.class));

        mockMvc.perform(post("/api/labs/events/orders")
                        .queryParam("strategy", "direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_BODY))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("EVENT_PUBLISH_FAILED"));

        org.assertj.core.api.Assertions.assertThat(count("orders")).isZero();
        org.assertj.core.api.Assertions.assertThat(
                jdbcTemplate.queryForObject(
                        "select stock_quantity from products where id = 1",
                        Integer.class
                )
        ).isEqualTo(120);
    }

    @Test
    void consumerSideEffectIsIdempotentByEventId() throws Exception {
        var event = new OrderPlacedEvent(
                UUID.randomUUID(),
                999L,
                "LAB-DUPLICATE",
                BigDecimal.TEN,
                Instant.parse("2026-08-10T00:00:00Z")
        );

        processingService.process(event);
        processingService.process(event);

        mockMvc.perform(get("/api/labs/events/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processed").value(1))
                .andExpect(jsonPath("$.data.duplicates").value(1));
    }

    @Test
    void resetRestoresKnownStockAndRemovesLabFixtures() throws Exception {
        mockMvc.perform(post("/api/labs/events/orders")
                        .queryParam("strategy", "outbox")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_BODY))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/labs/events/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reset").value(true));

        org.assertj.core.api.Assertions.assertThat(count("orders")).isZero();
        org.assertj.core.api.Assertions.assertThat(count("order_events_outbox")).isZero();
        org.assertj.core.api.Assertions.assertThat(
                jdbcTemplate.queryForObject(
                        "select stock_quantity from products where id = 1",
                        Integer.class
                )
        ).isEqualTo(120);
    }

    private long count(String table) {
        var count = jdbcTemplate.queryForObject("select count(*) from " + table, Long.class);
        return count == null ? 0 : count;
    }
}
