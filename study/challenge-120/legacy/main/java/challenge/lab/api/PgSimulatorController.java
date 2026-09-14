package challenge.lab.api;

import challenge.lab.infra.LabPaymentGateway;
import challenge.lab.infra.PgSimulatorService;
import challenge.payment.PaymentGateway;
import org.springframework.web.bind.annotation.*;

/** 오직 로컬 가짜 PG 조작용. 완료는 원장만 변경하며 알림 HTTP 전송은 별도 클라이언트가 한다. */
@RestController
@RequestMapping("/lab/pg")
public class PgSimulatorController {
    private final PgSimulatorService simulator;

    public PgSimulatorController(PgSimulatorService simulator) {
        this.simulator = simulator;
    }

    @PutMapping("/mode/{mode}")
    public void mode(@PathVariable("mode") LabPaymentGateway.Mode mode) {
        simulator.mode(mode);
    }

    @PostMapping("/{key}/complete")
    public PaymentGateway.Receipt complete(@PathVariable("key") String key) {
        return simulator.complete(key);
    }

    @GetMapping("/{key}")
    public PaymentGateway.Receipt find(@PathVariable("key") String key) {
        return simulator.find(key);
    }
}
