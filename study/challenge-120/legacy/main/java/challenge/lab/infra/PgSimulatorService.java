package challenge.lab.infra;

import challenge.payment.PaymentGateway;
import org.springframework.stereotype.Service;

/** 로컬 실험 제어용 facade. 운영 결제 유스케이스와 분리된 제공 장치다. */
@Service
public class PgSimulatorService {
    private final LabPaymentGateway gateway;

    public PgSimulatorService(LabPaymentGateway gateway) {
        this.gateway = gateway;
    }

    public void mode(LabPaymentGateway.Mode mode) {
        gateway.mode(mode);
    }

    public PaymentGateway.Receipt complete(String key) {
        return gateway.complete(key);
    }

    public PaymentGateway.Receipt find(String key) {
        return gateway.lookup(key)
                .orElseThrow(() -> new IllegalArgumentException("unknown PG key"));
    }
}
