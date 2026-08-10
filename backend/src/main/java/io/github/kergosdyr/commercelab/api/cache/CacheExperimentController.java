package io.github.kergosdyr.commercelab.api.cache;

import io.github.kergosdyr.commercelab.api.cache.response.CacheExperimentMetricsResponse;
import io.github.kergosdyr.commercelab.api.cache.response.CacheExperimentResponse;
import io.github.kergosdyr.commercelab.domain.cache.CacheExperimentService;
import io.github.kergosdyr.commercelab.domain.cache.CacheProductQuery;
import io.github.kergosdyr.commercelab.domain.cache.CacheStrategy;
import io.github.kergosdyr.commercelab.support.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/labs/cache")
public class CacheExperimentController {

    private final CacheExperimentService cacheExperimentService;

    public CacheExperimentController(CacheExperimentService cacheExperimentService) {
        this.cacheExperimentService = cacheExperimentService;
    }

    @GetMapping("/naive/products/{productId}")
    public ApiResponse<CacheExperimentResponse> readNaively(@PathVariable Long productId) {
        var result = cacheExperimentService.readProduct(new CacheProductQuery(productId, CacheStrategy.NAIVE));
        return ApiResponse.success(CacheExperimentResponse.fromResult(result));
    }

    @GetMapping("/protected/products/{productId}")
    public ApiResponse<CacheExperimentResponse> readWithProtection(@PathVariable Long productId) {
        var result = cacheExperimentService.readProduct(new CacheProductQuery(productId, CacheStrategy.PROTECTED));
        return ApiResponse.success(CacheExperimentResponse.fromResult(result));
    }

    @GetMapping("/metrics")
    public ApiResponse<CacheExperimentMetricsResponse> metrics() {
        return ApiResponse.success(CacheExperimentMetricsResponse.fromResult(cacheExperimentService.readMetrics()));
    }

    @PostMapping("/reset")
    public ApiResponse<CacheExperimentMetricsResponse> reset() {
        return ApiResponse.success(CacheExperimentMetricsResponse.fromResult(cacheExperimentService.reset()));
    }
}
