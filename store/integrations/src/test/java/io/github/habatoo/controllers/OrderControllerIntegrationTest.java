package io.github.habatoo.controllers;

import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.handlers.GlobalExceptionHandler;
import io.github.habatoo.servicies.OrderService;
import io.github.habatoo.utils.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockOidcLogin;

/**
 * Интеграционные тесты для OrderController с использованием @SpringBootTest.
 * Покрывают отображение списка заказов и отдельного заказа, а также обработку ошибок.
 */
@AutoConfigureWebTestClient
@Import(GlobalExceptionHandler.class)
@ImportAutoConfiguration(ThymeleafAutoConfiguration.class)
@DisplayName("Интеграционный SpringBootTest тест OrderController")
class OrderControllerIntegrationTest extends BaseTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OrderService orderService;

    /**
     * Тест отображения списка заказов.
     */
    @Test
    @DisplayName("GET /orders — успешный возврат списка заказов с корректной моделью и view")
    void getOrdersListSuccessTest() {
        List<OrderDto> orders = List.of(
                new OrderDto(1L, List.of(), BigDecimal.valueOf(100), LocalDateTime.now()),
                new OrderDto(2L, List.of(), BigDecimal.valueOf(200), LocalDateTime.now())
        );
        when(orderService.getOrders()).thenReturn(Flux.fromIterable(orders));

        webTestClient
                .mutateWith(mockOidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .mutateWith(csrf())
                .get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(res -> {
                    String html = res.getResponseBody();
                    assertNotNull(html);

                    assertTrue(html.contains("orders"));
                    assertTrue(html.contains("100"));
                    assertTrue(html.contains("200"));
                });
    }

    /**
     * Тест отображения отдельного заказа с флагом нового заказа.
     */
    @Test
    @DisplayName("GET /orders/{id}?order=true — успешный возврат заказа с флагом newOrder")
    void getOrderWithNewOrderFlagTest() {
        OrderDto orderDto = new OrderDto(11L, List.of(), BigDecimal.valueOf(555), LocalDateTime.now());
        when(orderService.getOrder(eq(11L), eq(true))).thenReturn(Mono.just(orderDto));

        webTestClient
                .mutateWith(mockOidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .mutateWith(csrf())
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/orders/11")
                        .queryParam("newOrder", "true")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(res -> {
                    String html = res.getResponseBody();
                    assertNotNull(html);
                    assertTrue(html.contains("555"));
                    assertTrue(html.contains("Поздравляем! Успешная покупка!"));
                });
    }

    /**
     * Тест — заказ не найден (возвращается страница ошибки 404).
     */
    @Test
    @DisplayName("GET /orders/{id} — заказ не найден, отображается error/404")
    void getOrderNotFoundTest() {
        when(orderService.getOrder(eq(404L), anyBoolean()))
                .thenReturn(Mono.error(new NoSuchElementException("Заказ не найден")));

        webTestClient
                .mutateWith(mockOidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .mutateWith(csrf())
                .get()
                .uri("/orders/404")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(res -> {
                    String html = res.getResponseBody();
                    assertNotNull(html);
                    assertTrue(html.contains("Ошибка 404"));
                });
    }

    /**
     * Тест — у анонимного пользователя ордера не обрабатываются..
     */
    @Test
    @DisplayName("GET /orders — анонимный пользователь, редирект на логин")
    void getOrderListUnauthorizedRedirectTest() {
        webTestClient
                .get()
                .uri("/orders")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/login");

        verifyNoInteractions(orderService);
    }

    /**
     * Тест — у анонимного пользователя ордера не обрабатываются..
     */
    @Test
    @DisplayName("GET /orders/{id} — анонимный пользователь, редирект на логин")
    void getOrderDetailsUnauthorizedRedirectTest() {
        webTestClient
                .get()
                .uri("/orders/1")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/login");

        verifyNoInteractions(orderService);
    }
}
