package io.github.kergosdyr.commercelab.domain.cache;

import java.time.Duration;

public interface ExperimentDelay {

    void pause(Duration duration);
}
