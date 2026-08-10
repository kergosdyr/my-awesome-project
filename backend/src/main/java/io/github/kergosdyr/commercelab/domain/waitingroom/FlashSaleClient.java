package io.github.kergosdyr.commercelab.domain.waitingroom;

public interface FlashSaleClient {

    Attempt tryPurchase();

    Metrics metrics();

    void reset();

    record Attempt(boolean accepted, long processingDurationMillis) {
    }

    record Metrics(long accepted, long rejected, int currentActive, int maxActive) {
    }
}
