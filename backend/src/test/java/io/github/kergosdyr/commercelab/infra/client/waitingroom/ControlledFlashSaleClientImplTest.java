package io.github.kergosdyr.commercelab.infra.client.waitingroom;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.Executors;

import io.github.kergosdyr.commercelab.domain.waitingroom.WaitingRoomTestFixtures.TestPolicy;
import org.junit.jupiter.api.Test;

class ControlledFlashSaleClientImplTest {

    @Test
    void rejectsWorkAboveTheConfiguredConcurrentCapacity() throws Exception {
        var policy = new TestPolicy(
                1,
                Duration.ofMillis(100),
                Duration.ofSeconds(30),
                Duration.ofSeconds(10),
                Duration.ofSeconds(5),
                Duration.ofMillis(10)
        );
        var client = new ControlledFlashSaleClientImpl(policy);

        try (var executor = Executors.newSingleThreadExecutor()) {
            var first = executor.submit(client::tryPurchase);
            for (var attempt = 0; attempt < 100 && client.metrics().currentActive() == 0; attempt++) {
                Thread.sleep(1);
            }

            var overloaded = client.tryPurchase();
            var accepted = first.get();

            assertThat(accepted.accepted()).isTrue();
            assertThat(overloaded.accepted()).isFalse();
            assertThat(client.metrics().accepted()).isEqualTo(1);
            assertThat(client.metrics().rejected()).isEqualTo(1);
            assertThat(client.metrics().maxActive()).isEqualTo(1);
        }
    }
}
