package io.github.habatoo.storages.impl;

import io.github.habatoo.dto.response.ItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.springframework.data.domain.Sort;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Юнит-тесты для {@link RedisItemListStorageImpl}.
 * Проверяются получение и сохранение списка товаров в Redis, а также корректная генерация ключей.
 */
@DisplayName("Тесты RedisItemListStorageImpl")
class RedisItemListStorageImplTest {

    private ReactiveRedisTemplate<String, List<ItemDto>> redisTemplate;
    private ReactiveValueOperations<String, List<ItemDto>> valueOperations;

    private RedisItemListStorageImpl storage;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(ReactiveRedisTemplate.class);
        valueOperations = mock(ReactiveValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        storage = new RedisItemListStorageImpl(
                1,
                redisTemplate
        );
    }

    @Test
    @DisplayName("Успешное получение списка товаров из Redis")
    void testGetItemsSuccess() {
        List<ItemDto> items = List.of(
                new ItemDto(1L, "A", null, "", BigDecimal.valueOf(100), 0),
                new ItemDto(2L, "B", null, "", BigDecimal.valueOf(200), 0));
        Sort sort = Sort.by("price");

        String key = "items:list:phone:10:0:" + sort;

        when(valueOperations.get(key))
                .thenReturn(Mono.just(items));

        Mono<List<ItemDto>> result = storage.getItems("phone", 10, 0, sort);

        StepVerifier.create(result)
                .expectNext(items)
                .verifyComplete();
    }

    @Test
    @DisplayName("Возврат пустого Mono при отсутствии данных в Redis")
    void testGetItemsEmpty() {
        Sort sort = Sort.by("price");
        String key = "items:list:search:5:2:" + sort;

        when(valueOperations.get(key))
                .thenReturn(Mono.empty());

        Mono<List<ItemDto>> result = storage.getItems("search", 5, 2, sort);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("Успешное сохранение списка товаров в Redis с TTL")
    void testSaveItemsSuccess() {
        List<ItemDto> items = List.of(
                new ItemDto(3L, "C", null, "", BigDecimal.valueOf(300), 0)
        );
        Sort sort = Sort.by("price");

        String key = "items:list:apple:20:1:" + sort;

        when(valueOperations.set(eq(key), eq(items), any()))
                .thenReturn(Mono.just(true));

        Mono<Boolean> result = storage.saveItems("apple", 20, 1, sort, items);

        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();

        verify(valueOperations, times(1))
                .set(eq(key), eq(items), any());
    }

    @Test
    @DisplayName("Корректная генерация ключа Redis")
    void testKeyGeneration() {
        Sort sort = Sort.by("name");

        when(valueOperations.get(anyString())).thenReturn(Mono.empty());

        storage.getItems("abc", 15, 3, sort).subscribe();

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).get(keyCaptor.capture());

        assertEquals("items:list:abc:15:3:" + sort, keyCaptor.getValue());
    }
}
