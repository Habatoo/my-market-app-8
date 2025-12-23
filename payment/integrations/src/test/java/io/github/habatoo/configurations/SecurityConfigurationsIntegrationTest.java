package io.github.habatoo.configurations;

import io.github.habatoo.handler.PaymentExceptionHandler;
import io.github.habatoo.utils.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@DisplayName("Интеграционный тест безопасности (SecurityConfigurations)")
@Import(PaymentExceptionHandler.class)
@AutoConfigureWebTestClient
class SecurityConfigurationsIntegrationTest extends BaseTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private SecurityConfigurations securityConfigurations;

    @Test
    @DisplayName("Бин SecurityConfigurations должен быть успешно загружен")
    void templateBeanShouldBeLoadedTest() {
        assertThat(securityConfigurations).isNotNull();
    }

    @Test
    @DisplayName("Анонимный запрос к любому эндпоинту должен возвращать 401")
    void anonymousRequestShouldReturn401Test() {
        webTestClient.get()
                .uri("/api/any-endpoint")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Запрос с валидным JWT должен проходить аутентификацию")
    void authenticatedRequestShouldNotReturn401Test() {
        webTestClient.mutateWith(mockJwt())
                .get()
                .uri("/api/any-random-path")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("POST запрос должен работать без CSRF токена")
    void postRequestShouldWorkWithoutCsrfTest() {
        webTestClient.mutateWith(mockJwt())
                .post()
                .uri("/payments/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Запрос к актуатору/здоровью тоже требует аутентификации (согласно anyExchange)")
    void healthEndpointShouldBeProtectedTest() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
