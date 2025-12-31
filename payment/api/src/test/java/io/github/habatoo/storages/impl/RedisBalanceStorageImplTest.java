package io.github.habatoo.storages.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Юнит-тесты для хранилища балансов в Redis.
 * Проверяют корректность формирования ключей и вызова реактивных операций Redis.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты RedisBalanceStorageImpl")
class RedisBalanceStorageImplTest {

    @Mock
    private ReactiveRedisTemplate<String, BigDecimal> redisTemplate;

    @Mock
    private ReactiveValueOperations<String, BigDecimal> valueOperations;

    private RedisBalanceStorageImpl redisBalanceStorage;

    private final String username = "testUser";
    private final String expectedKey = "balance:" + username;
    private final Integer ttlMinutes = 60;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        redisBalanceStorage = new RedisBalanceStorageImpl(ttlMinutes, redisTemplate);
    }

    /**
     * Тест получения баланса по имени пользователя.
     * Проверяет, что вызывается метод get с правильным префиксом ключа.
     */
    @Test
    @DisplayName("getBalanceByName: успешное получение значения по сформированному ключу")
    void getBalanceByNameSuccessTest() {
        BigDecimal expectedBalance = new BigDecimal("100.50");
        when(valueOperations.get(expectedKey)).thenReturn(Mono.just(expectedBalance));

        StepVerifier.create(redisBalanceStorage.getBalanceByName(username))
                .expectNext(expectedBalance)
                .verifyComplete();

        verify(valueOperations, times(1)).get(expectedKey);
    }

    /**
     * Тест поведения при отсутствии данных в Redis.
     */
    @Test
    @DisplayName("getBalanceByName: возврат Mono.empty(), если ключ не найден")
    void getBalanceByNameEmptyTest() {
        when(valueOperations.get(expectedKey)).thenReturn(Mono.empty());

        StepVerifier.create(redisBalanceStorage.getBalanceByName(username))
                .verifyComplete();
    }

    /**
     * Тест сохранения баланса.
     * Проверяет использование TTL и корректность передачи параметров в Redis.
     */
    @Test
    @DisplayName("saveBalanceByName: сохранение баланса с заданным TTL")
    void saveBalanceByNameSuccessTest() {
        BigDecimal balanceToSave = new BigDecimal("500.00");
        Duration expectedTtl = Duration.ofMinutes(ttlMinutes);
        when(valueOperations.set(eq(expectedKey), eq(balanceToSave), eq(expectedTtl)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(redisBalanceStorage.saveBalanceByName(username, balanceToSave))
                .expectNext(true)
                .verifyComplete();

        verify(valueOperations, times(1))
                .set(expectedKey, balanceToSave, expectedTtl);
    }

    /**
     * Тест обработки ошибки при записи в Redis.
     */
    @Test
    @DisplayName("saveBalanceByName: обработка ошибки при сбое в Redis")
    void saveBalanceByNameErrorTest() {
        BigDecimal balanceToSave = new BigDecimal("500.00");
        when(valueOperations.set(any(), any(), any()))
                .thenReturn(Mono.error(new RuntimeException("Redis connection error")));

        StepVerifier.create(redisBalanceStorage.saveBalanceByName(username, balanceToSave))
                .expectError(RuntimeException.class)
                .verify();
    }
}
