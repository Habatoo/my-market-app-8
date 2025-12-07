package io.github.habatoo.services;

import io.github.habatoo.payment.model.PaymentRequest;
import io.github.habatoo.payment.model.PaymentResponse;
import io.github.habatoo.services.impl.InMemoryBalanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Интеграционный тест PaymentsServiceImpl")
class PaymentsServiceImplIntegrationTest {

    @Autowired
    private PaymentsService paymentsService;

    @Autowired
    private InMemoryBalanceService balanceService;

    @BeforeEach
    void setup() {
        balanceService.reset(BigDecimal.valueOf(1000)).block();
    }

    @Test
    @DisplayName("Получение текущего баланса")
    void testGetBalance() {
        StepVerifier.create(paymentsService.getBalance())
                .assertNext(response -> assertThat(
                        response.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1000)))
                .verifyComplete();
    }

    @Test
    @DisplayName("Успешная оплата при достаточном балансе")
    void testPaySuccess() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(BigDecimal.valueOf(200));

        StepVerifier.create(paymentsService.pay(Mono.just(request)))
                .assertNext(response -> assertThat(
                        response.getStatus()).isEqualTo(PaymentResponse.StatusEnum.SUCCESS))
                .verifyComplete();

        StepVerifier.create(balanceService.getBalance())
                .assertNext(balance -> assertThat(balance).isEqualByComparingTo(BigDecimal.valueOf(800)))
                .verifyComplete();
    }

    @Test
    @DisplayName("Оплата неудачна при недостаточном балансе")
    void testPayFailed() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(BigDecimal.valueOf(2000));

        StepVerifier.create(paymentsService.pay(Mono.just(request)))
                .assertNext(response -> assertThat(
                        response.getStatus()).isEqualTo(PaymentResponse.StatusEnum.FAILED))
                .verifyComplete();

        StepVerifier.create(balanceService.getBalance())
                .assertNext(balance -> assertThat(balance).isEqualByComparingTo(BigDecimal.valueOf(1000)))
                .verifyComplete();
    }

    @Test
    @DisplayName("Несколько оплат подряд корректно уменьшают баланс")
    void testMultiplePayments() {
        PaymentRequest request1 = new PaymentRequest();
        request1.setAmount(BigDecimal.valueOf(300));

        PaymentRequest request2 = new PaymentRequest();
        request2.setAmount(BigDecimal.valueOf(400));

        StepVerifier.create(paymentsService.pay(Mono.just(request1)))
                .assertNext(response -> assertThat(
                        response.getStatus()).isEqualTo(PaymentResponse.StatusEnum.SUCCESS))
                .verifyComplete();

        StepVerifier.create(paymentsService.pay(Mono.just(request2)))
                .assertNext(response -> assertThat(
                        response.getStatus()).isEqualTo(PaymentResponse.StatusEnum.SUCCESS))
                .verifyComplete();

        StepVerifier.create(balanceService.getBalance())
                .assertNext(balance -> assertThat(balance).isEqualByComparingTo(BigDecimal.valueOf(300)))
                .verifyComplete();
    }
}
