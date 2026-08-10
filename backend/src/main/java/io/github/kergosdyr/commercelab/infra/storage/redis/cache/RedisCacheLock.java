package io.github.kergosdyr.commercelab.infra.storage.redis.cache;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import io.github.kergosdyr.commercelab.domain.cache.CacheLock;
import io.github.kergosdyr.commercelab.domain.cache.CacheStrategy;
import io.github.kergosdyr.commercelab.support.error.ApiException;
import io.github.kergosdyr.commercelab.support.error.ErrorType;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisCacheLock implements CacheLock {

    private static final String LOCK_PREFIX = "commerce-lab:cache-stampede:lock:";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisCacheLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<Token> tryAcquire(CacheStrategy strategy, Long productId, Duration lease) {
        var key = LOCK_PREFIX + strategy.name().toLowerCase(Locale.ROOT) + ":product:" + productId;
        var value = UUID.randomUUID().toString();
        try {
            var acquired = redisTemplate.opsForValue().setIfAbsent(key, value, lease);
            if (Boolean.TRUE.equals(acquired)) {
                return Optional.of(new Token(key, value));
            }
            return Optional.empty();
        } catch (DataAccessException exception) {
            throw new ApiException(ErrorType.CACHE_UNAVAILABLE);
        }
    }

    @Override
    public void release(Token token) {
        try {
            redisTemplate.execute(UNLOCK_SCRIPT, List.of(token.key()), token.value());
        } catch (DataAccessException exception) {
            throw new ApiException(ErrorType.CACHE_UNAVAILABLE);
        }
    }
}
