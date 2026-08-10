package io.github.kergosdyr.commercelab.infra.client.waitingroom;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import io.github.kergosdyr.commercelab.domain.waitingroom.FlashSaleClient;
import io.github.kergosdyr.commercelab.domain.waitingroom.WaitingRoomPolicy;
import org.springframework.stereotype.Component;

@Component
public class ControlledFlashSaleClientImpl implements FlashSaleClient {

    private final WaitingRoomPolicy policy;
    private final Semaphore capacity;
    private final AtomicInteger currentActive = new AtomicInteger();
    private final AtomicInteger maxActive = new AtomicInteger();
    private final AtomicLong accepted = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();

    public ControlledFlashSaleClientImpl(WaitingRoomPolicy policy) {
        this.policy = policy;
        this.capacity = new Semaphore(policy.maxConcurrency(), true);
    }

    @Override
    public Attempt tryPurchase() {
        if (!capacity.tryAcquire()) {
            rejected.incrementAndGet();
            return new Attempt(false, 0);
        }

        var startedAt = System.nanoTime();
        var active = currentActive.incrementAndGet();
        maxActive.accumulateAndGet(active, Math::max);
        try {
            Thread.sleep(policy.workDuration());
            accepted.incrementAndGet();
            return new Attempt(true, (System.nanoTime() - startedAt) / 1_000_000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Controlled flash-sale work was interrupted", exception);
        } finally {
            currentActive.decrementAndGet();
            capacity.release();
        }
    }

    @Override
    public Metrics metrics() {
        return new Metrics(
                accepted.get(),
                rejected.get(),
                currentActive.get(),
                maxActive.get()
        );
    }

    @Override
    public void reset() {
        accepted.set(0);
        rejected.set(0);
        maxActive.set(currentActive.get());
    }
}
