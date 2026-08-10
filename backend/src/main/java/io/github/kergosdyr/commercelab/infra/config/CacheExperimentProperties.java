package io.github.kergosdyr.commercelab.infra.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.labs.cache")
public record CacheExperimentProperties(
        Duration ttl,
        Duration originDelay,
        Duration lockLease,
        Duration lockWaitTimeout,
        Duration lockPollInterval
) {
}
