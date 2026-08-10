package io.github.kergosdyr.commercelab.infra.storage.redis.cache;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kergosdyr.commercelab.domain.cache.CacheStrategy;
import io.github.kergosdyr.commercelab.domain.cache.CachedProduct;
import io.github.kergosdyr.commercelab.domain.cache.ProductCache;
import io.github.kergosdyr.commercelab.domain.product.ProductStatus;
import io.github.kergosdyr.commercelab.support.error.ApiException;
import io.github.kergosdyr.commercelab.support.error.ErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

@Component
public class RedisProductCache implements ProductCache {

    private static final Logger log = LoggerFactory.getLogger(RedisProductCache.class);
    private static final String CACHE_PREFIX = "commerce-lab:cache-stampede:cache:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisProductCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<CachedProduct> read(CacheStrategy strategy, Long productId) {
        var key = key(strategy, productId);
        try {
            var json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }

            try {
                return Optional.of(objectMapper.readValue(json, RedisProductPayload.class).toDomain());
            } catch (JsonProcessingException exception) {
                redisTemplate.unlink(key);
                log.warn("Removed an unreadable cache-stampede value for key {}", key);
                return Optional.empty();
            }
        } catch (DataAccessException exception) {
            throw unavailable();
        }
    }

    @Override
    public void write(CacheStrategy strategy, CachedProduct product, Duration ttl) {
        try {
            var json = objectMapper.writeValueAsString(RedisProductPayload.fromDomain(product));
            redisTemplate.opsForValue().set(key(strategy, product.id()), json, ttl);
        } catch (JsonProcessingException | DataAccessException exception) {
            throw unavailable();
        }
    }

    @Override
    public void clearExperimentEntries() {
        try {
            var keys = new ArrayList<String>();
            var options = ScanOptions.scanOptions()
                    .match(CACHE_PREFIX + "*")
                    .count(100)
                    .build();
            try (var cursor = redisTemplate.scan(options)) {
                cursor.forEachRemaining(keys::add);
            }
            if (!keys.isEmpty()) {
                redisTemplate.unlink(keys);
            }
        } catch (DataAccessException exception) {
            throw unavailable();
        }
    }

    private String key(CacheStrategy strategy, Long productId) {
        return CACHE_PREFIX + strategy.name().toLowerCase(Locale.ROOT) + ":product:" + productId;
    }

    private ApiException unavailable() {
        return new ApiException(ErrorType.CACHE_UNAVAILABLE);
    }

    private record RedisProductPayload(
            Long id,
            String sku,
            String name,
            String description,
            BigDecimal price,
            int stockQuantity,
            ProductStatus status
    ) {

        private static RedisProductPayload fromDomain(CachedProduct product) {
            return new RedisProductPayload(
                    product.id(),
                    product.sku(),
                    product.name(),
                    product.description(),
                    product.price(),
                    product.stockQuantity(),
                    product.status()
            );
        }

        private CachedProduct toDomain() {
            return new CachedProduct(id, sku, name, description, price, stockQuantity, status);
        }
    }
}
