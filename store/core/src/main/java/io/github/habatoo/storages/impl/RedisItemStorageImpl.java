package io.github.habatoo.storages.impl;

import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.storages.RedisItemStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Сервис для кэширования и получения отдельных товаров (Item) в Redis.
 * Используется как кэш-слой для быстрого доступа к данным по идентификатору товара.
 *
 * <p>Ключом в Redis выступает идентификатор товара ({@code id}). Значением является
 * объект {@link ItemDto}, сохранённый с использованием настроенного времени жизни (TTL),
 * определяемого конфигурацией приложения.</p>
 *
 * <p>Все операции выполняются в реактивной парадигме (Project Reactor) и возвращают
 * реактивный тип {@code Mono}.</p>
 */
@Component
public class RedisItemStorageImpl implements RedisItemStorage {

    public static String CASH_KEY_PREFIX = "item:card:";

    private final Duration timeToLive;

    private final ReactiveRedisTemplate<String, ItemDto> itemRedisTemplate;

    public RedisItemStorageImpl(@Value("${application.redis-ttl-minutes}")
                                Integer timeToLive,
                                ReactiveRedisTemplate<String, ItemDto> itemRedisTemplate) {
        this.timeToLive = Duration.ofMinutes(timeToLive);
        this.itemRedisTemplate = itemRedisTemplate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ItemDto> getItem(Long id) {
        String key = obtainKey(id);
        return itemRedisTemplate.opsForValue().get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Boolean> saveItem(Long id, ItemDto dto) {
        String key = obtainKey(id);

        return itemRedisTemplate.opsForValue().set(key, dto, timeToLive);
    }

    private String obtainKey(Long id) {
        return String.format("%s%s", CASH_KEY_PREFIX, id);
    }
}
