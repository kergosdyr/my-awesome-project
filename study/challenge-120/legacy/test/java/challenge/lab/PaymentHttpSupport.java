package challenge.lab;

import challenge.lab.infra.LabPaymentGateway;
import challenge.payment.PaymentRepository;
import challenge.payment.Reservation;
import challenge.payment.ReservationRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
        classes = PaymentLabApplication.class,
        properties = "spring.config.name=payment-lab",
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class PaymentHttpSupport {
    static final boolean MYSQL = Boolean.getBoolean("challenge.mysql");
    static final MySQLContainer DB =
            MYSQL
                    ? new MySQLContainer("mysql:8.4")
                            .withDatabaseName("day006_payment")
                            .withUsername("challenge")
                            .withPassword("challenge")
                    : null;

    static {
        if (DB != null) DB.start();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry r) {
        r.add(
                "spring.datasource.url",
                () -> MYSQL ? DB.getJdbcUrl() : "jdbc:h2:mem:day006-http;DB_CLOSE_DELAY=-1");
        r.add("spring.datasource.username", () -> MYSQL ? DB.getUsername() : "sa");
        r.add("spring.datasource.password", () -> MYSQL ? DB.getPassword() : "");
    }

    @LocalServerPort int port;
    @Autowired PaymentRepository payments;
    @Autowired ReservationRepository reservations;
    @Autowired LabPaymentGateway gateway;
    final HttpClient client = HttpClient.newHttpClient();
    final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void seed() {
        payments.deleteAll();
        reservations.deleteAll();
        gateway.reset();
        reservations.save(new Reservation(1L));
        reservations.save(new Reservation(2L));
    }

    record Reply(int code, JsonNode body) {}

    Reply request(String method, String path, String body) throws Exception {
        var request =
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                        .header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(body))
                        .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new Reply(
                response.statusCode(),
                response.body().isBlank() ? json.nullNode() : json.readTree(response.body()));
    }

    Reply pay(long id) throws Exception {
        return request("POST", "/payments", "{\"reservationId\":" + id + ",\"amount\":1000}");
    }

    Reply notifyApproval(JsonNode receipt) throws Exception {
        return request("POST", "/payments/notifications", receipt.toString());
    }

    JsonNode pendingThenComplete() throws Exception {
        org.junit.jupiter.api.Assertions.assertEquals(
                200, request("PUT", "/lab/pg/mode/PROCESSING", "").code());
        var pending = pay(1);
        org.junit.jupiter.api.Assertions.assertEquals(202, pending.code());
        org.junit.jupiter.api.Assertions.assertEquals(
                "PENDING", pending.body().get("status").asText());
        var complete = request("POST", "/lab/pg/1/complete", "");
        org.junit.jupiter.api.Assertions.assertEquals(200, complete.code());
        return complete.body();
    }
}
