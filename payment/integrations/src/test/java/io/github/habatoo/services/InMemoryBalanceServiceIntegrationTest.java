package io.github.habatoo.services;

import io.github.habatoo.Application;
import io.github.habatoo.services.impl.InMemoryBalanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Интеграционный тест InMemoryBalanceService")
class InMemoryBalanceServiceIntegrationTest {

    @Autowired
    private InMemoryBalanceService balanceService;

    @BeforeEach
    void setup() {
        balanceService = new InMemoryBalanceService(BigDecimal.valueOf(1000));
    }

    @Test
    @DisplayName("Получение текущего баланса")
    void testGetBalance() {
        StepVerifier.create(balanceService.getBalance())
                .assertNext(balance -> assertThat(balance).isEqualByComparingTo(BigDecimal.valueOf(1000)))
                .verifyComplete();
    }

    @Test
    @DisplayName("Уменьшение баланса")
    void testDecreaseBalance() {
        BigDecimal amountToDecrease = BigDecimal.valueOf(200);

        StepVerifier.create(balanceService.decrease(amountToDecrease))
                .assertNext(newBalance -> assertThat(newBalance).isEqualByComparingTo(BigDecimal.valueOf(800)))
                .verifyComplete();

        StepVerifier.create(balanceService.getBalance())
                .assertNext(balance -> assertThat(balance).isEqualByComparingTo(BigDecimal.valueOf(800)))
                .verifyComplete();
    }

    @Test
    @DisplayName("Уменьшение баланса несколько раз")
    void testDecreaseMultipleTimes() {
        StepVerifier.create(balanceService.decrease(BigDecimal.valueOf(100)))
                .assertNext(balance -> assertThat(balance).isEqualByComparingTo(BigDecimal.valueOf(900)))
                .verifyComplete();

        StepVerifier.create(balanceService.decrease(BigDecimal.valueOf(300)))
                .assertNext(balance -> assertThat(balance).isEqualByComparingTo(BigDecimal.valueOf(600)))
                .verifyComplete();

        StepVerifier.create(balanceService.getBalance())
                .assertNext(balance -> assertThat(balance).isEqualByComparingTo(BigDecimal.valueOf(600)))
                .verifyComplete();
    }

    @Test
    @DisplayName("Получение установка текущего баланса")
    void testResetBalance() {
        StepVerifier.create(balanceService.reset(BigDecimal.valueOf(100)))
                .verifyComplete();
    }
}
