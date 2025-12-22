package io.github.habatoo.storages.impl;

import io.github.habatoo.storages.RedisBalanceStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Реализация для кэширования и получения отдельных товаров (Item) в Redis.
 * Используется как кэш-слой для быстрого доступа к данным по идентификатору товара.
 *
 * <p>Ключом в Redis выступает идентификатор товара ({@code id}). Значением является
 * объект {@link java.math.BigDecimal}, сохранённый с использованием настроенного времени жизни (TTL),
 * определяемого конфигурацией приложения.</p>
 *
 * <p>Все операции выполняются в реактивной парадигме (Project Reactor) и возвращают
 * реактивный тип {@code Mono}.</p>
 */
@Slf4j
@Service
public class RedisBalanceStorageImpl implements RedisBalanceStorage {

    private static String CACHE_KEY_PREFIX = "balance:";

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
