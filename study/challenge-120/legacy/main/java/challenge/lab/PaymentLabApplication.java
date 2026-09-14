package challenge.lab;

import challenge.payment.Payment;
import challenge.payment.PaymentRepository;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Day5 결제 코드를 그대로 조립한 로컬 HTTP 실습. 실제 PG·금전 거래는 없다. */
@SpringBootApplication(scanBasePackages = {"challenge.lab", "challenge.payment"})
@EntityScan(basePackageClasses = Payment.class)
@EnableJpaRepositories(basePackageClasses = PaymentRepository.class)
public class PaymentLabApplication {
    public static void main(String[] args) {
        var application = new SpringApplication(PaymentLabApplication.class);
        application.setDefaultProperties(Map.of("spring.config.name", "payment-lab"));
        application.run(args);
    }
}
