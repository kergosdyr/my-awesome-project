package io.github.kergosdyr.commercelab.support.monitoring;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class EventLabMetrics {

    private final AtomicLong created = new AtomicLong();
    private final AtomicLong published = new AtomicLong();
    private final AtomicLong duplicates = new AtomicLong();
    private final AtomicLong publishFailures = new AtomicLong();

    public void recordCreatedAfterCommit() {
        afterCommit(created::incrementAndGet);
    }

    public void recordPublishedAfterCommit() {
        afterCommit(published::incrementAndGet);
    }

    public void recordDuplicateAfterCommit() {
        afterCommit(duplicates::incrementAndGet);
    }

    public void recordPublishFailureNow() {
        publishFailures.incrementAndGet();
    }

    public void resetAfterCommit() {
        afterCommit(() -> {
            created.set(0);
            published.set(0);
            duplicates.set(0);
            publishFailures.set(0);
        });
    }

    public Snapshot snapshot() {
        return new Snapshot(
                created.get(),
                published.get(),
                duplicates.get(),
                publishFailures.get()
        );
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    public record Snapshot(
            long created,
            long published,
            long duplicates,
            long publishFailures
    ) {
    }
}
