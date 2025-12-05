package io.github.habatoo.controllers;

import io.github.habatoo.payment.model.BalanceResponse;
import io.github.habatoo.payment.model.PaymentRequest;
import io.github.habatoo.payment.model.PaymentResponse;
import io.github.habatoo.services.PaymentsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для PaymentsController.
 * Покрывает методы работы с балансом и совершением платежей.
 * Используется WebFluxTest для имитации HTTP-запросов к контроллеру и проверки корректности модели и view.
 */
@WebFluxTest(PaymentsController.class)
@ContextConfiguration(classes = PaymentsController.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Тесты unit-уровня методов PaymentsController с использованием WebFluxTest")
public class PaymentsControllerCashedTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PaymentsService paymentsService;

    @MockitoBean
    private ServerWebExchange exchange;

    @Test
    @DisplayName("POST /payments/payment - SUCCESS")
    void createPaymentSuccess() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(BigDecimal.valueOf(100));

        PaymentResponse response = new PaymentResponse();
        response.setStatus(PaymentResponse.StatusEnum.SUCCESS);

        when(paymentsService.pay(any(Mono.class))).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/payments/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PaymentResponse.class)
                .value(resp -> {
                    assert resp.getStatus() == PaymentResponse.StatusEnum.SUCCESS;
                });
    }

    @Test
    @DisplayName("POST /payments/payment - FAILED")
    void createPaymentFailed() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(BigDecimal.valueOf(500));

        PaymentResponse response = new PaymentResponse();
        response.setStatus(PaymentResponse.StatusEnum.FAILED);

        when(paymentsService.pay(any(Mono.class))).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/payments/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PaymentResponse.class)
                .value(resp -> {
                    assert resp.getStatus() == PaymentResponse.StatusEnum.FAILED;
                });
    }

    @Test
    @DisplayName("GET /payments/balance - SUCCESS")
    void getWalletBalance() {
        BalanceResponse balanceResponse = new BalanceResponse();
        balanceResponse.setBalance(BigDecimal.valueOf(1000));

        when(paymentsService.getBalance()).thenReturn(Mono.just(balanceResponse));

        webTestClient.get()
                .uri("/payments/balance")
                .exchange()
                .expectStatus().isCreated()
                .expectBody(BalanceResponse.class)
                .value(resp -> assertEquals(0, resp.getBalance().compareTo(BigDecimal.valueOf(1000))));
    }
}
