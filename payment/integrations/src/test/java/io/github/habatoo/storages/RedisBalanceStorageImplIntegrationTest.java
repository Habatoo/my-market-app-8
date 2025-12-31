package io.github.habatoo.storages;

import io.github.habatoo.storages.impl.RedisBalanceStorageImpl;
import io.github.habatoo.utils.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Интеграционный тест RedisBalanceStorageImpl")
class RedisBalanceStorageImplIntegrationTest extends BaseTest {

    @Autowired
    private RedisBalanceStorageImpl balanceStorage;

    @Autowired
    private ReactiveRedisTemplate<String, BigDecimal> balanceRedisTemplate;

    @Test
    @DisplayName("Успешное сохранение и получение баланса")
    void saveAndGetBalanceTest() {
        String username = "storage-user-1";
        BigDecimal balance = new BigDecimal("150.75");

        StepVerifier.create(balanceStorage.saveBalanceByName(username, balance))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(balanceStorage.getBalanceByName(username))
                .assertNext(val -> assertThat(val).isEqualByComparingTo(balance))
                .verifyComplete();
    }

    @Test
    @DisplayName("Возврат пустой Mono, если ключа не существует")
    void getNonExistingBalanceTest() {
        String username = "ghost-user";

        StepVerifier.create(balanceStorage.getBalanceByName(username))
                .verifyComplete();
    }

    @Test
    @DisplayName("Обновление существующего баланса")
    void updateExistingBalanceTest() {
        String username = "storage-user-2";
        BigDecimal firstBalance = new BigDecimal("100.00");
        BigDecimal secondBalance = new BigDecimal("200.00");

        StepVerifier.create(balanceStorage.saveBalanceByName(username, firstBalance)
                        .then(balanceStorage.saveBalanceByName(username, secondBalance))
                        .then(balanceStorage.getBalanceByName(username)))
                .assertNext(val -> assertThat(val).isEqualByComparingTo(secondBalance))
                .verifyComplete();
    }

    @Test
    @DisplayName("Проверка правильности формирования ключа в Redis")
    void verifyRedisKeyPrefixTest() {
        String username = "prefix-user";
        BigDecimal balance = new BigDecimal("777.00");
        String expectedKey = "balance:" + username;

        StepVerifier.create(balanceStorage.saveBalanceByName(username, balance))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(balanceRedisTemplate.opsForValue().get(expectedKey))
                .assertNext(val -> assertThat(val).isEqualByComparingTo(balance))
                .verifyComplete();
    }
}
