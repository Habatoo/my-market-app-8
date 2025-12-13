package io.github.habatoo.storages.impl;

import io.github.habatoo.dto.response.ItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Юнит-тесты для RedisItemStorageImpl.
 * Проверяют корректность формирования ключей, работы get/save
 * и взаимодействие с ReactiveRedisTemplate.
 */
@DisplayName("Юнит-тесты RedisItemStorageImpl")
class RedisItemStorageImplTest {

    private ReactiveRedisTemplate<String, ItemDto> redisTemplate;
    private ReactiveValueOperations<String, ItemDto> valueOps;
    private RedisItemStorageImpl storage;

    @BeforeEach
    void setUp() {
        redisTemplate = Mockito.mock(ReactiveRedisTemplate.class);
        valueOps = Mockito.mock(ReactiveValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        storage = new RedisItemStorageImpl(
                5,
                redisTemplate
        );
    }

    /**
     * Вспомогательный метод для построения ключа,
     * аналогичный приватному obtainKey.
     */
    private String key(long id) {
        return RedisItemStorageImpl.CACHE_KEY_PREFIX + id;
    }

    @Nested
    @DisplayName("Метод getItem")
    class GetItemTests {

        /**
         * Проверяет, что getItem вызывает Redis GET с правильным ключом.
         */
        @Test
        @DisplayName("Получение элемента по ключу — успешный сценарий")
        void getItemSuccessTest() {
            long id = 123L;
            ItemDto dto = new ItemDto(id, "A", null, "", BigDecimal.valueOf(100), 0);

            when(valueOps.get(eq(key(id)))).thenReturn(Mono.just(dto));

            StepVerifier.create(storage.getItem(id))
                    .expectNext(dto)
                    .verifyComplete();

            verify(valueOps, times(1)).get(eq(key(id)));
        }

        /**
         * Проверяет, что при отсутствии данных в Redis возвращается empty Mono.
         */
        @Test
        @DisplayName("Получение элемента по ключу — отсутствует в Redis")
        void getItemNotFoundTest() {
            long id = 55L;

            when(valueOps.get(eq(key(id)))).thenReturn(Mono.empty());

            StepVerifier.create(storage.getItem(id))
                    .verifyComplete();

            verify(valueOps, times(1)).get(eq(key(id)));
        }
    }

    @Nested
    @DisplayName("Метод saveItem")
    class SaveItemTests {
        /**
         * Проверяет корректный вызов Redis SET с TTL и возвращаемое значение.
         */
        @Test
        @DisplayName("Сохранение элемента — успешный сценарий")
        void saveItemSuccessTest() {
            long id = 77L;
            ItemDto dto = new ItemDto(id, "A", null, "", BigDecimal.valueOf(100), 0);
            Duration ttl = Duration.ofMinutes(5);

            when(valueOps.set(eq(key(id)), eq(dto), eq(ttl)))
                    .thenReturn(Mono.just(true));

            StepVerifier.create(storage.saveItem(id, dto))
                    .expectNext(true)
                    .verifyComplete();

            verify(valueOps, times(1))
                    .set(eq(key(id)), eq(dto), eq(ttl));
        }

        /**
         * Проверяет обработку ложного результата Redis SET.
         */
        @Test
        @DisplayName("Сохранение элемента — Redis вернул false")
        void saveItemFalseFromRedisTest() {
            long id = 99L;
            ItemDto dto = new ItemDto(id, "A", null, "", BigDecimal.valueOf(100), 0);
            Duration ttl = Duration.ofMinutes(5);

            when(valueOps.set(eq(key(id)), eq(dto), eq(ttl)))
                    .thenReturn(Mono.just(false));

            StepVerifier.create(storage.saveItem(id, dto))
                    .expectNext(false)
                    .verifyComplete();

            verify(valueOps, times(1))
                    .set(eq(key(id)), eq(dto), eq(ttl));
        }
    }
}
