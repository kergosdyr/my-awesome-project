package challenge.payment;

import java.util.*;

/** 외부 승인 원장은 Spring DB 트랜잭션에 참여하지 않는다. 지연·유실은 sleep 없이 재현한다. */
public class FakePaymentGateway implements PaymentGateway {
    public enum Mode {
        NORMAL,
        APPROVE_THEN_LOSE_RESPONSE,
        APPROVE_THEN_ABORT,
        PROCESSING
    }

    /** 요청 중단·로컬 롤백 관찰용. 실제 JVM 종료가 아니며 업무 코드에서 잡는 대상이 아니다. */
    public static class SimulatedProcessStop extends Error {}

    private final Map<String, Receipt> ledger = new LinkedHashMap<>();
    private Mode mode = Mode.NORMAL;
    private int approvals, approveCalls, lookupCalls;

    public synchronized void reset() {
        ledger.clear();
        mode = Mode.NORMAL;
        approvals = approveCalls = lookupCalls = 0;
    }

    public synchronized void mode(Mode mode) {
        this.mode = mode;
    }

    @Override
    public synchronized Receipt approve(String key, long reservationId, long amount) {
        approveCalls++;
        if (key == null || key.isBlank() || amount <= 0) throw new IllegalArgumentException();
        var existing = ledger.get(key);
        if (existing != null) {
            if (existing.reservationId() != reservationId || existing.amount() != amount)
                throw new IllegalArgumentException("key conflict");
            return existing;
        }
        var status = mode == Mode.PROCESSING ? Status.PROCESSING : Status.APPROVED;
        var receipt =
                new Receipt(
                        key,
                        reservationId,
                        amount,
                        status,
                        status == Status.APPROVED ? "approval-" + (++approvals) : null);
        ledger.put(key, receipt);
        if (mode == Mode.APPROVE_THEN_ABORT) throw new SimulatedProcessStop();
        if (mode == Mode.APPROVE_THEN_LOSE_RESPONSE) throw new ResponseLostException();
        return receipt;
    }

    @Override
    public synchronized Optional<Receipt> lookup(String key) {
        lookupCalls++;
        return Optional.ofNullable(ledger.get(key));
    }

    public synchronized Receipt onlyReceipt() {
        if (ledger.size() != 1)
            throw new AssertionError("expected one external payment, got " + ledger.size());
        return ledger.values().iterator().next();
    }

    public synchronized Receipt complete(String key) {
        var previous = Objects.requireNonNull(ledger.get(key));
        if (previous.status() == Status.APPROVED) return previous;
        var receipt =
                new Receipt(
                        key,
                        previous.reservationId(),
                        previous.amount(),
                        Status.APPROVED,
                        "approval-" + (++approvals));
        ledger.put(key, receipt);
        return receipt;
    }

    public synchronized int approvals() {
        return approvals;
    }

    public synchronized int approveCalls() {
        return approveCalls;
    }

    public synchronized int lookupCalls() {
        return lookupCalls;
    }
}
