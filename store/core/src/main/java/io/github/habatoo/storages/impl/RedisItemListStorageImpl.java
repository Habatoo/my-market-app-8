package io.github.habatoo.storages.impl;

import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.storages.RedisItemListStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * Сервис для хранения и получения списков товаров в Redis.
 * Используется как кэш-слой для результатов поиска по каталогу.
 *
 * <p>Ключ кэша формируется на основании параметров поиска: исходной строки запроса,
 * размера страницы, номера страницы и параметров сортировки. Значением является список DTO
 * элементов каталога, сохранённый в Redis с заданным временем жизни (TTL), определяемым
 * конфигурацией приложения.</p>
 *
 * <p>Все операции выполняются в реактивной парадигме (Project Reactor) и возвращают
 * соответствующие типы {@code Mono}.</p>
 */
@Component
public class RedisItemListStorageImpl implements RedisItemListStorage {

    public static String CASH_KEY_PREFIX = "items:list:";

    private final Duration timeToLive;

    private final ReactiveRedisTemplate<String, List<ItemDto>> itemsListRedisTemplate;

    public RedisItemListStorageImpl(@Value("${application.redis-ttl-minutes}")
                                    Integer timeToLive,
                                    ReactiveRedisTemplate<String, List<ItemDto>> itemsListRedisTemplate) {
        this.timeToLive = Duration.ofMinutes(timeToLive);
        this.itemsListRedisTemplate = itemsListRedisTemplate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<List<ItemDto>> getItems(
            String rawSearch,
            int pageSize,
            int pageNumber,
            Sort sort) {
        String key = obtainKey(
                rawSearch,
                pageSize,
                pageNumber,
                sort);
        return itemsListRedisTemplate.opsForValue().get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Boolean> saveItems(
            String rawSearch,
            int pageSize,
            int pageNumber,
            Sort sort, List<ItemDto> dtos) {
        String key = obtainKey(
                rawSearch,
                pageSize,
                pageNumber,
                sort);

        return itemsListRedisTemplate.opsForValue().set(key, dtos, timeToLive);
    }

    private String obtainKey(
            String rawSearch,
            int pageSize,
            int pageNumber,
            Sort sort) {

        return String.format("%s%s:%s:%s:%s", CASH_KEY_PREFIX, rawSearch, pageSize, pageNumber, sort);
    }
}
