package io.github.habatoo.services.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

/**
 * Unit-тесты для {@link InMemoryBalanceService}.
 * <p>
 * Тестируем базовые сценарии получения и изменения баланса.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тест работы методов InMemoryBalanceServiceTest")
class InMemoryBalanceServiceTest {

    private static final BigDecimal INITIAL_BALANCE = BigDecimal.valueOf(100);

    /**
     * Проверяет, что начальный баланс корректно возвращается методом {@link InMemoryBalanceService#getBalance()}.
     */
    @Test
    @DisplayName("getBalance() — возвращает первоначально установленное значение")
    void getBalanceReturnsInitialValueTest() {
        var service = new InMemoryBalanceService(INITIAL_BALANCE);

        StepVerifier.create(service.getBalance())
                .expectNext(INITIAL_BALANCE)
                .verifyComplete();
    }

    /**
     * Проверяет, что вызов {@link InMemoryBalanceService#decrease(BigDecimal)}
     * уменьшает текущий баланс на заданную сумму.
     */
    @Test
    @DisplayName("decrease() — корректно уменьшает баланс")
    void decreaseSubtractsAmountCorrectlyTest() {
        var service = new InMemoryBalanceService(INITIAL_BALANCE);

        StepVerifier.create(service.decrease(BigDecimal.valueOf(10)))
                .expectNext(INITIAL_BALANCE.subtract(BigDecimal.TEN))
                .verifyComplete();
    }

    /**
     * Проверяет последовательные уменьшения баланса
     * и устойчивость хранимого состояния.
     */
    @Test
    @DisplayName("decrease() — несколько последовательных изменений обновляют баланс корректно")
    void decreaseMultipleSequentialUpdatesTest() {
        var service = new InMemoryBalanceService(INITIAL_BALANCE);

        StepVerifier.create(service.decrease(BigDecimal.valueOf(10)))
                .expectNext(BigDecimal.valueOf(90))
                .verifyComplete();

        StepVerifier.create(service.decrease(BigDecimal.valueOf(25)))
                .expectNext(BigDecimal.valueOf(65))
                .verifyComplete();

        StepVerifier.create(service.getBalance())
                .expectNext(BigDecimal.valueOf(65))
                .verifyComplete();
    }

    /**
     * Проверяет, что вычитание суммы больше текущего баланса допускается
     * (не блокирует, не кидает исключений).
     * Правильность поведения зависит от бизнес-логики,
     * но текущая реализация разрешает отрицательный баланс.
     */
    @Test
    @DisplayName("decrease() — допускает уход баланса в отрицательные значения")
    void decreaseHandlesNegativeBalanceTest() {
        var service = new InMemoryBalanceService(BigDecimal.TEN);

        StepVerifier.create(service.decrease(BigDecimal.valueOf(30)))
                .expectNext(BigDecimal.valueOf(-20))
                .verifyComplete();

        StepVerifier.create(service.getBalance())
                .expectNext(BigDecimal.valueOf(-20))
                .verifyComplete();
    }
}
