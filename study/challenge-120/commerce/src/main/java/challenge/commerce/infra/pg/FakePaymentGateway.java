package challenge.commerce.infra.pg;

import challenge.commerce.domain.payment.PaymentGateway;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** 제공 PG 시뮬레이터. 로컬 DB 트랜잭션 밖의 메모리 원장이며 재시작하면 초기화된다. */
@Component
public class FakePaymentGateway implements PaymentGateway {
    public enum Mode {
        NORMAL,
        PROCESSING,
        APPROVE_THEN_LOSE_RESPONSE
    }

    private final Map<String, Receipt> ledger = new LinkedHashMap<>();
    private Mode mode = Mode.NORMAL;
    private int approvals;

    public synchronized void mode(Mode mode) {
        this.mode = mode;
    }

    public synchronized void reset() {
        ledger.clear();
        mode = Mode.NORMAL;
        approvals = 0;
    }

    public synchronized int approvals() {
        return approvals;
    }

    @Override
    public synchronized Receipt approve(String key, long orderId, long amount) {
        var previous = ledger.get(key);
        if (previous != null) {
            if (previous.orderId() != orderId || previous.amount() != amount) {
                throw new IllegalArgumentException("same key requires same payload");
            }
            return previous;
        }
        var status = mode == Mode.PROCESSING ? Status.PROCESSING : Status.APPROVED;
        var receipt = new Receipt(
                key, orderId, amount, status, status == Status.APPROVED ? "commerce-approval-" + ++approvals : null);
        ledger.put(key, receipt);
        if (mode == Mode.APPROVE_THEN_LOSE_RESPONSE) throw new ResponseLostException();
        return receipt;
    }

    @Override
    public synchronized Optional<Receipt> lookup(String key) {
        return Optional.ofNullable(ledger.get(key));
    }

    public synchronized Receipt complete(String key) {
        var previous = lookup(key).orElseThrow(() -> new IllegalArgumentException("unknown PG key"));
        if (previous.approved()) return previous;
        var receipt = new Receipt(
                key, previous.orderId(), previous.amount(), Status.APPROVED, "commerce-approval-" + ++approvals);
        ledger.put(key, receipt);
        return receipt;
    }
}
