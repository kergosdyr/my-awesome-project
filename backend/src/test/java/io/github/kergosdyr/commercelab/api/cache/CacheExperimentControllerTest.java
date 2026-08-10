package io.github.kergosdyr.commercelab.api.cache;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import io.github.kergosdyr.commercelab.api.GlobalExceptionHandler;
import io.github.kergosdyr.commercelab.domain.cache.CacheExperimentMetricsResult;
import io.github.kergosdyr.commercelab.domain.cache.CacheExperimentPolicy;
import io.github.kergosdyr.commercelab.domain.cache.CacheExperimentResult;
import io.github.kergosdyr.commercelab.domain.cache.CacheExperimentService;
import io.github.kergosdyr.commercelab.domain.cache.CacheProductQuery;
import io.github.kergosdyr.commercelab.domain.cache.CacheStrategy;
import io.github.kergosdyr.commercelab.domain.cache.CachedProduct;
import io.github.kergosdyr.commercelab.domain.product.ProductStatus;
import io.github.kergosdyr.commercelab.support.error.ApiException;
import io.github.kergosdyr.commercelab.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CacheExperimentControllerTest {

    @Mock
    private CacheExperimentService cacheExperimentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CacheExperimentController(cacheExperimentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsTheNaiveExperimentResultInTheStandardEnvelope() throws Exception {
        var query = new CacheProductQuery(1L, CacheStrategy.NAIVE);
        when(cacheExperimentService.readProduct(query)).thenReturn(new CacheExperimentResult(
                CacheStrategy.NAIVE,
                CacheExperimentResult.Outcome.ORIGIN_LOADED,
                product()
        ));

        mockMvc.perform(get("/api/labs/cache/naive/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.strategy").value("NAIVE"))
                .andExpect(jsonPath("$.data.outcome").value("ORIGIN_LOADED"))
                .andExpect(jsonPath("$.data.product.id").value(1));

        verify(cacheExperimentService).readProduct(query);
    }

    @Test
    void exposesAndResetsPerStrategyMetrics() throws Exception {
        var metrics = metrics();
        when(cacheExperimentService.readMetrics()).thenReturn(metrics);
        when(cacheExperimentService.reset()).thenReturn(metrics());

        mockMvc.perform(get("/api/labs/cache/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cacheTtlMs").value(500))
                .andExpect(jsonPath("$.data.originDelayMs").value(40))
                .andExpect(jsonPath("$.data.strategies", hasSize(2)))
                .andExpect(jsonPath("$.data.strategies[0].requests").value(10));

        mockMvc.perform(post("/api/labs/cache/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(cacheExperimentService).readMetrics();
        verify(cacheExperimentService).reset();
    }

    @Test
    void mapsAProtectedLockTimeoutToTheErrorEnvelope() throws Exception {
        var query = new CacheProductQuery(1L, CacheStrategy.PROTECTED);
        when(cacheExperimentService.readProduct(query))
                .thenThrow(new ApiException(ErrorType.CACHE_LOCK_TIMEOUT));

        mockMvc.perform(get("/api/labs/cache/protected/products/1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("CACHE_LOCK_TIMEOUT"));
    }

    private CacheExperimentMetricsResult metrics() {
        var policy = new CacheExperimentPolicy(
                Duration.ofMillis(500),
                Duration.ofMillis(40),
                Duration.ofSeconds(2),
                Duration.ofSeconds(1),
                Duration.ofMillis(10)
        );
        return new CacheExperimentMetricsResult(
                policy,
                List.of(
                        new CacheExperimentMetricsResult.StrategyMetrics(
                                CacheStrategy.NAIVE, 10, 8, 2, 2, 0
                        ),
                        new CacheExperimentMetricsResult.StrategyMetrics(
                                CacheStrategy.PROTECTED, 10, 8, 2, 1, 1
                        )
                )
        );
    }

    private CachedProduct product() {
        return new CachedProduct(
                1L,
                "BEAN-HAEUNDAE-1KG",
                "해운대 블렌드 원두 1kg",
                "테스트 설명",
                BigDecimal.valueOf(28_900),
                120,
                ProductStatus.ACTIVE
        );
    }
}
