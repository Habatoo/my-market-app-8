package io.github.habatoo.storages.impl;

import io.github.habatoo.storages.RedisBalanceStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;

@Slf4j
@Service
public class RedisBalanceStorageImpl implements RedisBalanceStorage {

    public static String CACHE_KEY_PREFIX = "balance:";

    private final Duration timeToLive;

    private final ReactiveRedisTemplate<String, BigDecimal> balanceRedisTemplate;

    public RedisBalanceStorageImpl(@Value("${application.redis-ttl-minutes}")
                                   Integer timeToLive,
                                   ReactiveRedisTemplate<String, BigDecimal> balanceRedisTemplate) {
        this.timeToLive = Duration.ofMinutes(timeToLive);
        this.balanceRedisTemplate = balanceRedisTemplate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<BigDecimal> getBalanceByName(String username) {
        String key = CACHE_KEY_PREFIX + username;
        return balanceRedisTemplate.opsForValue().get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Boolean> saveBalanceByName(String username, BigDecimal balance) {
        String key = CACHE_KEY_PREFIX + username;
        return balanceRedisTemplate.opsForValue().set(key, balance, timeToLive);
    }
}
