package io.github.habatoo.controllers;

import io.github.habatoo.payment.model.BalanceResponse;
import io.github.habatoo.payment.model.PaymentRequest;
import io.github.habatoo.payment.model.PaymentResponse;
import io.github.habatoo.services.PaymentsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Юнит тесты методов работы с балансом и совершением платежей.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тест работы методов PaymentsController")
class PaymentsControllerTest {

    @Mock
    private PaymentsService paymentsService;

    @Mock
    private ServerWebExchange exchange;

    @InjectMocks
    private PaymentsController paymentsController;

    @Test
    @DisplayName("Тест создание платежа успешный")
    void createPaymentSuccessTest() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(BigDecimal.valueOf(100));

        PaymentResponse response = new PaymentResponse();
        response.setStatus(PaymentResponse.StatusEnum.SUCCESS);

        when(paymentsService.pay(any(Mono.class)))
                .thenReturn(Mono.just(response));

        Mono<ResponseEntity<PaymentResponse>> result = paymentsController.createPayment(Mono.just(request), exchange);

        StepVerifier.create(result)
                .expectNextMatches(resp -> resp.getStatusCode() == HttpStatus.CREATED
                        && resp.getBody().getStatus() == PaymentResponse.StatusEnum.SUCCESS)
                .verifyComplete();
    }

    @Test
    @DisplayName("Тест создание платежа не успешный")
    void createPaymentFailedTest() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(BigDecimal.valueOf(500));

        PaymentResponse response = new PaymentResponse();
        response.setStatus(PaymentResponse.StatusEnum.FAILED);

        when(paymentsService.pay(any(Mono.class)))
                .thenReturn(Mono.just(response));

        Mono<ResponseEntity<PaymentResponse>> result = paymentsController.createPayment(Mono.just(request), exchange);

        StepVerifier.create(result)
                .expectNextMatches(resp -> resp.getStatusCode() == HttpStatus.CREATED
                        && resp.getBody().getStatus() == PaymentResponse.StatusEnum.FAILED)
                .verifyComplete();
    }

    @Test
    @DisplayName("Тест получения баланса успешный")
    void getWalletBalanceSuccessTest() {
        BalanceResponse balanceResponse = new BalanceResponse();
        balanceResponse.setBalance(BigDecimal.valueOf(1000));

        when(paymentsService.getBalance()).thenReturn(Mono.just(balanceResponse));

        Mono<ResponseEntity<BalanceResponse>> result = paymentsController.getWalletBalance(exchange);

        StepVerifier.create(result)
                .expectNextMatches(resp -> resp.getStatusCode() == HttpStatus.CREATED
                        && resp.getBody().getBalance().compareTo(BigDecimal.valueOf(1000)) == 0)
                .verifyComplete();
    }
}
