package io.github.habatoo;

import io.github.habatoo.payment.model.PaymentRequest;
import io.github.habatoo.payment.model.PaymentResponse;
import io.github.habatoo.services.BalanceService;
import io.github.habatoo.services.PaymentsService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StorePaymentIntegrationTest {

    @Autowired
    private PaymentsService paymentsService;

    @Autowired
    private BalanceService balanceService;

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2.4-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        String r2dbcUrl = String.format(
                "r2dbc:postgresql://%s:%d/%s",
                postgres.getHost(),
                postgres.getMappedPort(5432),
                postgres.getDatabaseName()
        );
        registry.add("spring.r2dbc.url", () -> r2dbcUrl);
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("payment.url", () -> "http://localhost:" + 8081);
        registry.add("application.redis-ttl-minutes", () -> 1);


        String redisUrl = String.format("%s:%d", redis.getHost(), redis.getMappedPort(6379));
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @Test
    @Order(1)
    @DisplayName("Оплата заказа через PaymentsService")
    void payOrderTest() {
        PaymentRequest request = new PaymentRequest()
                .amount(new BigDecimal("100.00"));

        Mono<PaymentResponse> paymentMono = paymentsService.pay(Mono.just(request));

        StepVerifier.create(paymentMono)
                .assertNext(response -> {
                    Assertions.assertNotNull(response);
                    Assertions.assertEquals(PaymentResponse.StatusEnum.SUCCESS, response.getStatus());
                })
                .verifyComplete();
    }
}
