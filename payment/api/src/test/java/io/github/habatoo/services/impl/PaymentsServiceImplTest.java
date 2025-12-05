package io.github.habatoo.services.impl;

import io.github.habatoo.payment.model.PaymentRequest;
import io.github.habatoo.payment.model.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Юнит тесты сервиса работы с балансом и совершением платежей.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тест работы методов PaymentsServiceImpl")
class PaymentsServiceImplTest {

    private PaymentsServiceImpl paymentsService;

    @BeforeEach
    void setUp() {
        AtomicReference<BigDecimal> balance = new AtomicReference<>(BigDecimal.valueOf(100));
        paymentsService = new PaymentsServiceImpl(balance);
    }

    @Test
    @DisplayName("getBalance возвращает текущий баланс")
    void testGetBalance() {
        StepVerifier.create(paymentsService.getBalance())
                .assertNext(response -> assertEquals(
                        BigDecimal.valueOf(100), response.getBalance()))
                .verifyComplete();
    }

    @Test
    @DisplayName("pay успешно списывает сумму с баланса")
    void testPaySuccess() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(BigDecimal.valueOf(50));

        StepVerifier.create(paymentsService.pay(Mono.just(request)))
                .assertNext(response -> assertEquals(
                        PaymentResponse.StatusEnum.SUCCESS, response.getStatus()))
                .verifyComplete();

        StepVerifier.create(paymentsService.getBalance())
                .assertNext(response -> assertEquals(
                        BigDecimal.valueOf(50), response.getBalance()))
                .verifyComplete();
    }

    @Test
    @DisplayName("pay возвращает FAILED при недостатке средств")
    void testPayFailed() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(BigDecimal.valueOf(200));

        StepVerifier.create(paymentsService.pay(Mono.just(request)))
                .assertNext(response -> assertEquals(
                        PaymentResponse.StatusEnum.FAILED, response.getStatus()))
                .verifyComplete();

        StepVerifier.create(paymentsService.getBalance())
                .assertNext(response -> assertEquals(
                        BigDecimal.valueOf(100), response.getBalance()))
                .verifyComplete();
    }

    @Test
    @DisplayName("pay списывает полностью баланс при точной сумме")
    void testPayExactBalance() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(BigDecimal.valueOf(100)); // ровно баланс

        StepVerifier.create(paymentsService.pay(Mono.just(request)))
                .assertNext(response -> assertEquals(
                        PaymentResponse.StatusEnum.SUCCESS, response.getStatus()))
                .verifyComplete();

        StepVerifier.create(paymentsService.getBalance())
                .assertNext(response -> assertEquals(
                        BigDecimal.ZERO, response.getBalance()))
                .verifyComplete();
    }

    @Test
    @DisplayName("pay с несколькими последовательными платежами корректно обновляет баланс")
    void testMultiplePayments() {
        PaymentRequest request1 = new PaymentRequest();
        request1.setAmount(BigDecimal.valueOf(30));

        PaymentRequest request2 = new PaymentRequest();
        request2.setAmount(BigDecimal.valueOf(50));

        StepVerifier.create(paymentsService.pay(Mono.just(request1)))
                .assertNext(response -> assertEquals(
                        PaymentResponse.StatusEnum.SUCCESS, response.getStatus()))
                .verifyComplete();

        StepVerifier.create(paymentsService.pay(Mono.just(request2)))
                .assertNext(response -> assertEquals(
                        PaymentResponse.StatusEnum.SUCCESS, response.getStatus()))
                .verifyComplete();

        StepVerifier.create(paymentsService.getBalance())
                .assertNext(response -> assertEquals(
                        BigDecimal.valueOf(20), response.getBalance()))
                .verifyComplete();
    }
}
