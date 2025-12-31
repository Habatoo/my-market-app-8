package io.github.habatoo.services;

import io.github.habatoo.payment.model.PaymentRequest;
import io.github.habatoo.payment.model.PaymentResponse;
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

@DisplayName("Интеграционный тест PaymentsServiceImpl")
class PaymentsServiceImplIntegrationTest extends BaseTest {

    @Autowired
    private PaymentsService paymentsService;

    @Autowired
    private RedisBalanceService balanceService;

    @Autowired
    private RedisBalanceStorage redisBalanceStorage;

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
        StepVerifier.create(paymentsService.getBalance()
                        .contextWrite(createJwtContext(username)))
                .assertNext(response -> assertThat(
                        response.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(300)))
                .verifyComplete();
    }

    @Test
    @DisplayName("Успешная оплата при достаточном балансе")
    void testPaySuccess() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(BigDecimal.valueOf(200));

        StepVerifier.create(paymentsService.pay(Mono.just(request))
                        .contextWrite(createJwtContext(username)))
                .assertNext(response -> assertThat(
                        response.getStatus()).isEqualTo(PaymentResponse.StatusEnum.SUCCESS))
                .verifyComplete();

        StepVerifier.create(balanceService.getBalance()
                        .contextWrite(createJwtContext(username)))
                .assertNext(balance -> assertThat(balance).isEqualByComparingTo(BigDecimal.valueOf(100)))
                .verifyComplete();
    }

    @Test
    @DisplayName("Оплата неудачна при недостаточном балансе")
    void testPayFailed() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(BigDecimal.valueOf(2000));

        StepVerifier.create(paymentsService.pay(Mono.just(request))
                        .contextWrite(createJwtContext(username)))
                .assertNext(response -> assertThat(
                        response.getStatus()).isEqualTo(PaymentResponse.StatusEnum.FAILED))
                .verifyComplete();

        StepVerifier.create(balanceService.getBalance()
                        .contextWrite(createJwtContext(username)))
                .assertNext(balance -> assertThat(balance).isEqualByComparingTo(BigDecimal.valueOf(300)))
                .verifyComplete();
    }

    @Test
    @DisplayName("Несколько оплат подряд корректно уменьшают баланс")
    void testMultiplePayments() {
        PaymentRequest request1 = new PaymentRequest();
        request1.setAmount(BigDecimal.valueOf(50));

        PaymentRequest request2 = new PaymentRequest();
        request2.setAmount(BigDecimal.valueOf(200));

        StepVerifier.create(balanceService.getBalance()
                        .contextWrite(createJwtContext(username)))
                .assertNext(balance -> assertThat(balance).isEqualByComparingTo(BigDecimal.valueOf(300)))
                .verifyComplete();

        StepVerifier.create(paymentsService.pay(Mono.just(request1))
                        .contextWrite(createJwtContext(username)))
                .assertNext(response -> assertThat(
                        response.getStatus()).isEqualTo(PaymentResponse.StatusEnum.SUCCESS))
                .verifyComplete();

        StepVerifier.create(paymentsService.pay(Mono.just(request2))
                        .contextWrite(createJwtContext(username)))
                .assertNext(response -> assertThat(
                        response.getStatus()).isEqualTo(PaymentResponse.StatusEnum.SUCCESS))
                .verifyComplete();

        StepVerifier.create(balanceService.getBalance()
                        .contextWrite(createJwtContext(username)))
                .assertNext(balance -> assertThat(balance).isEqualByComparingTo(BigDecimal.valueOf(50)))
                .verifyComplete();
    }
}
