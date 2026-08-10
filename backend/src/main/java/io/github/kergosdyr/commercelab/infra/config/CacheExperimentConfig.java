package io.github.kergosdyr.commercelab.infra.config;

import io.github.kergosdyr.commercelab.domain.cache.CacheExperimentPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CacheExperimentProperties.class)
public class CacheExperimentConfig {

    @Bean
    CacheExperimentPolicy cacheExperimentPolicy(CacheExperimentProperties properties) {
        return new CacheExperimentPolicy(
                properties.ttl(),
                properties.originDelay(),
                properties.lockLease(),
                properties.lockWaitTimeout(),
                properties.lockPollInterval()
        );
    }
}
