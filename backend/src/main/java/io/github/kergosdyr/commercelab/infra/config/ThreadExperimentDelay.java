package io.github.kergosdyr.commercelab.infra.config;

import java.time.Duration;

import io.github.kergosdyr.commercelab.domain.cache.ExperimentDelay;
import io.github.kergosdyr.commercelab.support.error.ApiException;
import io.github.kergosdyr.commercelab.support.error.ErrorType;
import org.springframework.stereotype.Component;

@Component
public class ThreadExperimentDelay implements ExperimentDelay {

    @Override
    public void pause(Duration duration) {
        if (duration.isZero()) {
            return;
        }

        try {
            Thread.sleep(duration);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiException(ErrorType.INTERNAL_SERVER_ERROR, "캐시 실험 대기가 중단되었습니다.");
        }
    }
}
