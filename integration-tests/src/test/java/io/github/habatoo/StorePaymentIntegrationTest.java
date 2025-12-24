package io.github.habatoo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Map;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockOidcLogin;

@DisplayName("Интеграционный тест ежмодульного взаимодействия")
public class StorePaymentIntegrationTest extends BaseTest {

    @Test
    @DisplayName("Успешная оплата заказа при наличии средств")
    void shouldProcessOrderSuccessfullyTest() {
        var purchaseRequest = Map.of(
                "userId", "user-123",
                "itemId", "item-456",
                "amount", 100.0
        );

        storeClient
                .mutateWith(mockOidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .mutateWith(csrf())
                .post()
                .uri("/buy")
                .bodyValue(purchaseRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo("PAID");
    }

    @Test
    @DisplayName("Ошибка заказа, если Payment вернул отказ (Insufficient Funds)")
    void shouldFailOrderIfPaymentIsRejectedTest() {
        var poorUserRequest = Map.of(
                "userId", "poor-user",
                "amount", 999999.99
        );

        storeClient
                .mutateWith(mockOidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .mutateWith(csrf())
                .post()
                .uri("/buy")
                .bodyValue(poorUserRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Payment failed");
    }
}
