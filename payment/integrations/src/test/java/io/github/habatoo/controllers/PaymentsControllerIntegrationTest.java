package io.github.habatoo.controllers;

import io.github.habatoo.exceptions.PaymentServiceUnavailableException;
import io.github.habatoo.handler.PaymentExceptionHandler;
import io.github.habatoo.payment.model.BalanceResponse;
import io.github.habatoo.payment.model.PaymentRequest;
import io.github.habatoo.payment.model.PaymentResponse;
import io.github.habatoo.services.PaymentsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(PaymentsController.class)
@Import(PaymentExceptionHandler.class)
@DisplayName("Интеграционный WebFlux тест PaymentsController")
public class PaymentsControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PaymentsService paymentsService;

    @Test
    @DisplayName("POST /payments/payment — успешная оплата")
    void testCreatePaymentSuccess() {
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
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(PaymentResponse.class)
                .value(body -> {
                    org.assertj.core.api.Assertions.assertThat(body.getStatus()).isEqualTo(PaymentResponse.StatusEnum.SUCCESS);
                });
    }

    @Test
    @DisplayName("POST /payments/payment — недостаточно средств, FAILED")
    void testCreatePaymentFailed() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(BigDecimal.valueOf(1000));

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
                .value(body -> assertThat(body.getStatus()).isEqualTo(PaymentResponse.StatusEnum.FAILED));
    }

    @Test
    @DisplayName("GET /payments/balance — возвращает баланс")
    void testGetWalletBalance() {
        BigDecimal balance = BigDecimal.valueOf(500);
        BalanceResponse balanceResponse = new BalanceResponse();
        balanceResponse.setBalance(balance);

        when(paymentsService.getBalance()).thenReturn(Mono.just(balanceResponse));

        webTestClient.get()
                .uri("/payments/balance")
                .exchange()
                .expectStatus().isCreated()
                .expectBody(BalanceResponse.class)
                .value(body -> assertThat(body.getBalance()).isEqualTo(balance));
    }

    @Test
    @DisplayName("POST /payments/payment — сервис недоступен, 503")
    void testCreatePaymentServiceUnavailable() {
        when(paymentsService.pay(any(Mono.class)))
                .thenReturn(Mono.error(new PaymentServiceUnavailableException()));

        PaymentRequest request = new PaymentRequest();
        request.setAmount(BigDecimal.valueOf(100));

        webTestClient.post()
                .uri("/payments/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody(String.class)
                .value(body -> assertThat(body).isEqualTo("Сервис временно недоступен"));
    }
}

