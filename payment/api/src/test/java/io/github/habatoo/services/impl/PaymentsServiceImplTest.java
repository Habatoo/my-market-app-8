package io.github.habatoo.services.impl;

import io.github.habatoo.payment.model.PaymentRequest;
import io.github.habatoo.payment.model.PaymentResponse;
import io.github.habatoo.services.BalanceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для {@link PaymentsServiceImpl}.
 * <p>
 * Покрываем сценарии:
 * — чтение баланса;
 * — успешная транзакция;
 * — неуспешная транзакция при недостатке средств;
 * — корректная работа с зависимостью BalanceService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тест работы методов PaymentsServiceImplTest")
class PaymentsServiceImplTest {

    @Mock
    private BalanceService balanceService;

    @InjectMocks
    private PaymentsServiceImpl paymentsService;

    /**
     * Проверяет, что {@link PaymentsServiceImpl#getBalance()} возвращает корректный DTO.
     */
    @Test
    @DisplayName("getBalance() — возвращает корректный BalanceResponse")
    void getBalanceReturnsCorrectResponseTest() {
        var expected = BigDecimal.valueOf(150);
        when(balanceService.getBalance()).thenReturn(Mono.just(expected));

        StepVerifier.create(paymentsService.getBalance())
                .assertNext(resp -> {
                    assert resp != null;
                    assert resp.getBalance().equals(expected);
                })
                .verifyComplete();

        verify(balanceService, times(1)).getBalance();
    }


    /**
     * Проверяет успешное проведение платежа:
     * — баланс достаточный;
     * — decrease() вызывается;
     * — возвращается статус SUCCESS.
     */
    @Test
    @DisplayName("pay() — успешный платеж, когда средств достаточно")
    void paySuccessWhenEnoughFundsTest() {
        var request = new PaymentRequest().amount(BigDecimal.valueOf(40));

        when(balanceService.getBalance()).thenReturn(Mono.just(BigDecimal.valueOf(100)));
        when(balanceService.decrease(BigDecimal.valueOf(40)))
                .thenReturn(Mono.just(BigDecimal.valueOf(60)));

        StepVerifier.create(paymentsService.pay(Mono.just(request)))
                .assertNext(resp -> {
                    assert resp.getStatus() == PaymentResponse.StatusEnum.SUCCESS;
                })
                .verifyComplete();

        verify(balanceService, times(1)).getBalance();
        verify(balanceService, times(1)).decrease(BigDecimal.valueOf(40));
    }


    /**
     * Проверяет неуспешное списание:
     * — средств недостаточно;
     * — decrease() НЕ вызывается;
     * — возвращается статус FAILED.
     */
    @Test
    @DisplayName("pay() — FAILED, если средств недостаточно")
    void payFailedWhenNotEnoughFundsTest() {
        var request = new PaymentRequest().amount(BigDecimal.valueOf(200));

        when(balanceService.getBalance()).thenReturn(Mono.just(BigDecimal.valueOf(100)));

        StepVerifier.create(paymentsService.pay(Mono.just(request)))
                .assertNext(resp -> {
                    assert resp.getStatus() == PaymentResponse.StatusEnum.FAILED;
                })
                .verifyComplete();

        verify(balanceService, times(1)).getBalance();
        verify(balanceService, never()).decrease(any());
    }


    /**
     * Проверяет последовательные вызовы:
     * — первый платеж успешный,
     * — второй — неуспешный.
     * Демонстрирует корректную цепочку реактивных вызовов.
     */
    @Test
    @DisplayName("pay() — корректно отрабатывает разные вызовы подряд")
    void payMultipleDifferentOutcomesTest() {
        var smallRequest = new PaymentRequest().amount(BigDecimal.valueOf(20));
        var largeRequest = new PaymentRequest().amount(BigDecimal.valueOf(200));

        when(balanceService.getBalance())
                .thenReturn(Mono.just(BigDecimal.valueOf(100)))
                .thenReturn(Mono.just(BigDecimal.valueOf(80)));
        when(balanceService.decrease(BigDecimal.valueOf(20)))
                .thenReturn(Mono.just(BigDecimal.valueOf(80)));

        StepVerifier.create(paymentsService.pay(Mono.just(smallRequest)))
                .assertNext(resp -> assertEquals(PaymentResponse.StatusEnum.SUCCESS, resp.getStatus()))
                .verifyComplete();

        StepVerifier.create(paymentsService.pay(Mono.just(largeRequest)))
                .assertNext(resp -> assertEquals(PaymentResponse.StatusEnum.FAILED, resp.getStatus()))
                .verifyComplete();

        verify(balanceService, times(2)).getBalance();
        verify(balanceService, times(1)).decrease(BigDecimal.valueOf(20));
    }

    @Test
    @DisplayName("Сброс баланса на корректное значение")
    void testResetBalance() {
        balanceService = new InMemoryBalanceService(BigDecimal.valueOf(500));

        StepVerifier.create(balanceService.reset(BigDecimal.valueOf(1000)))
                .verifyComplete();

        StepVerifier.create(balanceService.getBalance())
                .assertNext(balance -> assertThat(balance).isEqualByComparingTo(BigDecimal.valueOf(1000)))
                .verifyComplete();
    }

    @Test
    @DisplayName("Сброс баланса на отрицательное значение — ошибка")
    void testResetNegativeBalance() {
        balanceService = new InMemoryBalanceService(BigDecimal.valueOf(500));

        StepVerifier.create(balanceService.reset(BigDecimal.valueOf(-100)))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                                throwable.getMessage().equals("Баланс не может быть отрицательным"))
                .verify();
    }
}

