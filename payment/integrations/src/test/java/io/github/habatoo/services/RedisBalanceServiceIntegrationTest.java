package io.github.habatoo.services;

import io.github.habatoo.services.impl.RedisBalanceService;
import io.github.habatoo.storages.RedisBalanceStorage;
import io.github.habatoo.utils.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Интеграционный тест RedisBalanceService")
public class RedisBalanceServiceIntegrationTest extends BaseTest {

    @Autowired
    private RedisBalanceStorage redisBalanceStorage;

    @Autowired
    private RedisBalanceService balanceService;

    private String username;

    @BeforeEach
    void setup() {
        username = "active-user";
        balanceService.reset(new BigDecimal("300.00"))
                .contextWrite(createJwtContext(username))
                .block();
    }

    @Test
    @DisplayName("Получение текущего баланса")
    void testGetBalance() {
        StepVerifier.create(balanceService.getBalance()
                        .contextWrite(createJwtContext(username)))
                .assertNext(balance -> assertThat(balance).isEqualByComparingTo(new BigDecimal("300.00")))
                .verifyComplete();
    }

    @Test
    @DisplayName("Уменьшение баланса")
    void testDecreaseBalance() {
        BigDecimal amountToDecrease = new BigDecimal("100.00");

        Mono<BigDecimal> testChain = balanceService.decrease(amountToDecrease)
                .contextWrite(createJwtContext(username))
                .then(balanceService.getBalance())
                .contextWrite(createJwtContext(username));

        StepVerifier.create(testChain)
                .expectNext(new BigDecimal("200.00"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Получение установка текущего баланса")
    void testResetBalance() {
        StepVerifier.create(balanceService.reset(BigDecimal.valueOf(100)))
                .verifyComplete();
    }

    @Test
    @DisplayName("Ошибка при установке отрицательного баланса")
    void resetNegativeBalanceErrorTest() {
        StepVerifier.create(balanceService.reset(new BigDecimal("-10.00")))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("Запрос баланса без контекста безопасности")
    void getBalanceUnauthenticatedTest() {
        StepVerifier.create(balanceService.getBalance())
                .verifyComplete();
    }

    @Test
    @DisplayName("Списание средств без контекста безопасности")
    void decreaseUnauthenticatedTest() {
        StepVerifier.create(balanceService.decrease(new BigDecimal("50.00")))
                .verifyComplete();
    }
}
