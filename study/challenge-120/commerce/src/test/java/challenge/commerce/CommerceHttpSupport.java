package challenge.commerce;

import static org.junit.jupiter.api.Assertions.*;

import challenge.commerce.infra.db.*;
import challenge.commerce.infra.pg.FakePaymentGateway;
import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(classes = CommerceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class CommerceHttpSupport {
    static final boolean MYSQL = Boolean.getBoolean("challenge.mysql");
    static final MySQLContainer DB = MYSQL
            ? new MySQLContainer("mysql:8.4")
                    .withDatabaseName("commerce_test")
                    .withUsername("challenge")
                    .withPassword("challenge")
            : null;

    static {
        if (DB != null) DB.start();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add(
                "spring.datasource.url", () -> MYSQL ? DB.getJdbcUrl() : "jdbc:h2:mem:commerce-test;DB_CLOSE_DELAY=-1");
        properties.add("spring.datasource.username", () -> MYSQL ? DB.getUsername() : "sa");
        properties.add("spring.datasource.password", () -> MYSQL ? DB.getPassword() : "");
    }

    @LocalServerPort
    int port;

    @Autowired
    PaymentJpaRepository payments;

    @Autowired
    challenge.commerce.domain.payment.PaymentRepository paymentStore;

    @Autowired
    OrderJpaRepository orders;

    @Autowired
    ProductOptionJpaRepository options;

    @Autowired
    ProductJpaRepository products;

    @Autowired
    CatalogFixture fixture;

    @Autowired
    FakePaymentGateway gateway;

    final HttpClient client = HttpClient.newHttpClient();
    final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void reset() {
        payments.deleteAll();
        orders.deleteAll();
        options.deleteAll();
        products.deleteAll();
        fixture.run();
        gateway.reset();
    }

    record Reply(int code, JsonNode body) {}

    HttpResponse<String> raw(String method, String path, String body) throws Exception {
        return client.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                        .header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    Reply request(String method, String path, String body) throws Exception {
        var response = raw(method, path, body);
        return new Reply(
                response.statusCode(), response.body().isBlank() ? json.nullNode() : json.readTree(response.body()));
    }

    long order() throws Exception {
        var response = request(
                "POST", "/api/orders", "{\"items\":[{\"displayedUnitPrice\":129000,\"optionId\":101,\"quantity\":1}]}");
        assertEquals(201, response.code());
        return response.body().get("id").asLong();
    }

    Reply pay(long id) throws Exception {
        return request("POST", "/api/orders/" + id + "/payments", "");
    }

    Reply details(long id) throws Exception {
        return request("GET", "/api/orders/" + id, "");
    }

    Reply notifyApproval(JsonNode receipt) throws Exception {
        return request("POST", "/api/payments/notifications", receipt.toString());
    }

    JsonNode pendingThenComplete(long id) throws Exception {
        assertEquals(200, request("PUT", "/dev/pg/mode/PROCESSING", "").code());
        assertEquals(202, pay(id).code());
        var response = request("POST", "/dev/pg/" + id + "/complete", "");
        assertEquals(200, response.code());
        assertEquals("APPROVED", response.body().get("status").asText());
        return response.body();
    }
}
