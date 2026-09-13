package challenge.payment;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest(classes = PaymentApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(PaymentTestSupport.Fakes.class)
abstract class PaymentTestSupport {
    static final boolean MYSQL = Boolean.getBoolean("challenge.mysql");
    static final MySQLContainer DB;
    static {
        DB = MYSQL ? new MySQLContainer("mysql:8.4").withDatabaseName("day005").withUsername("challenge").withPassword("challenge") : null;
        if (DB != null) DB.start();
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> MYSQL ? DB.getJdbcUrl() : "jdbc:h2:mem:day005;MODE=MySQL;DB_CLOSE_DELAY=-1");
        r.add("spring.datasource.username", () -> MYSQL ? DB.getUsername() : "sa");
        r.add("spring.datasource.password", () -> MYSQL ? DB.getPassword() : "");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        r.add("spring.sql.init.mode", () -> "never");
    }
    @TestConfiguration static class Fakes {
        @Bean FakePaymentGateway gateway() { return new FakePaymentGateway(); }
    }
}
