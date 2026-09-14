package challenge.commerce.api;

import challenge.commerce.domain.payment.PaymentGateway;
import challenge.commerce.infra.pg.FakePaymentGateway;
import challenge.commerce.support.BusinessException;
import org.springframework.web.bind.annotation.*;

/** 개발용 시뮬레이터 제어 경계. 상품 구매 UI와 분리되어 있고 서버는 loopback에만 바인딩한다. */
@RestController
@RequestMapping("/dev/pg")
public class DevelopmentPgController {
    private final FakePaymentGateway gateway;

    public DevelopmentPgController(FakePaymentGateway gateway) {
        this.gateway = gateway;
    }

    @PutMapping("/mode/{mode}")
    public void mode(@PathVariable("mode") FakePaymentGateway.Mode mode) {
        gateway.mode(mode);
    }

    @PostMapping("/{key}/complete")
    public PaymentGateway.Receipt complete(@PathVariable("key") String key) {
        find(key);
        return gateway.complete(key);
    }

    @GetMapping("/{key}")
    public PaymentGateway.Receipt find(@PathVariable("key") String key) {
        return gateway.lookup(key)
                .orElseThrow(() -> BusinessException.notFound("PG 거래를 찾을 수 없습니다."));
    }
}
