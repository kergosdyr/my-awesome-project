package io.github.kergosdyr.commercelab.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommerceApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void listsTheSixSeedProductsInTheSuccessEnvelope() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data", hasSize(6)))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].sku").value("BEAN-HAEUNDAE-1KG"))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data[5].status").value("SOLD_OUT"));
    }

    @Test
    void readsOneProductAsAStableHotKey() throws Exception {
        mockMvc.perform(get("/api/products/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.price").value(129000));
    }

    @Test
    void returnsTheErrorEnvelopeWhenAProductDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/products/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void placesAnOrderAndReservesStock() throws Exception {
        var body = """
                {
                  "customerName": "  김코덱스  ",
                  "lines": [
                    {"productId": 1, "quantity": 2},
                    {"productId": 2, "quantity": 1}
                  ]
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.orderNumber").isNotEmpty())
                .andExpect(jsonPath("$.data.customerName").value("김코덱스"))
                .andExpect(jsonPath("$.data.totalAmount").value(76700))
                .andExpect(jsonPath("$.data.status").value("PLACED"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data.lines", hasSize(2)))
                .andExpect(jsonPath("$.data.lines[0].sku").value("BEAN-HAEUNDAE-1KG"))
                .andExpect(jsonPath("$.data.lines[0].lineAmount").value(57800));

        var remainingStock = jdbcTemplate.queryForObject(
                "select stock_quantity from products where id = 1",
                Integer.class
        );
        var savedLines = jdbcTemplate.queryForObject("select count(*) from order_lines", Integer.class);

        org.assertj.core.api.Assertions.assertThat(remainingStock).isEqualTo(118);
        org.assertj.core.api.Assertions.assertThat(savedLines).isEqualTo(2);
    }

    @Test
    void rejectsDuplicateLinesThroughTheErrorEnvelope() throws Exception {
        var body = """
                {
                  "customerName": "김코덱스",
                  "lines": [
                    {"productId": 3, "quantity": 1},
                    {"productId": 3, "quantity": 1}
                  ]
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("INVALID_ORDER_LINES"));
    }

    @Test
    void rejectsSoldOutProductsWithoutCreatingAnOrder() throws Exception {
        var body = """
                {
                  "customerName": "김코덱스",
                  "lines": [{"productId": 6, "quantity": 1}]
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("PRODUCT_NOT_FOR_SALE"));
    }
}
