package io.github.habatoo.controllers;

import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.handlers.GlobalExceptionHandler;
import io.github.habatoo.servicies.BuyService;
import io.github.habatoo.servicies.CartService;
import io.github.habatoo.servicies.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Интеграционный тест BuyController (@WebFluxTest) — покрытие всех крайних кейсов.
 * Покрывает: успешную покупку (заказ создан), покупку без заказов, ошибки/исключения на каждом шаге.
 */
@WebFluxTest(BuyController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("Интеграционный WebFlux тест BuyController")
class BuyControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private BuyService buyService;

    @MockitoBean
    private CartService cartService;

    /**
     * Успешная покупка: есть корзина, buy срабатывает, заказ возвращается.
     */
    @Test
    @DisplayName("POST /buy — успешная покупка, редирект на новый заказ")
    void buySuccessTest() {
        CartDto cartDto = new CartDto(42L, List.of(), BigDecimal.TEN);
        when(cartService.getItemsInTheCart()).thenReturn(Mono.just(cartDto));
        doNothing().when(buyService).buy(42L);

        OrderDto lastOrder = new OrderDto(111L, List.of(), BigDecimal.TEN, LocalDateTime.now());
        when(orderService.getOrders()).thenReturn(Flux.just(lastOrder));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders/111?newOrder=true");

        verify(cartService).getItemsInTheCart();
        verify(buyService).buy(42L);
        verify(orderService).getOrders();
    }

    /**
     * Покупка: корзина есть, buy, но заказ не создан — редирект на основную страницу заказов.
     */
    @Test
    @DisplayName("POST /buy — нет заказов, редирект на /orders/")
    void buyNoOrderTest() {
        CartDto cartDto = new CartDto(15L, List.of(), BigDecimal.ZERO);
        when(cartService.getItemsInTheCart()).thenReturn(Mono.just(cartDto));
        doNothing().when(buyService).buy(15L);
        when(orderService.getOrders()).thenReturn(Flux.empty());

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders/");

        verify(cartService).getItemsInTheCart();
        verify(buyService).buy(15L);
        verify(orderService).getOrders();
    }

    /**
     * Ошибка получения корзины — обработка исключения, возврат страницы ошибки.
     */
    @Test
    @DisplayName("POST /buy — корзина не найдена, выбрасывается исключение")
    void buyCartErrorTest() {
        when(cartService.getItemsInTheCart()).thenReturn(Mono.error(new IllegalStateException("Корзина не найдена")));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String view = result.getResponseBody();
                    assertTrue(view.contains("Корзина не найдена"));
                });
    }

    /**
     * Ошибка создания заказа — обработка исключения сервиса покупки.
     */
    @Test
    @DisplayName("POST /buy — ошибка БД при создании заказа")
    void buyServiceErrorTest() {
        CartDto cartDto = new CartDto(55L, List.of(), BigDecimal.TEN);
        when(cartService.getItemsInTheCart()).thenReturn(Mono.just(cartDto));
        doThrow(new DataAccessResourceFailureException("DB Ошибка")).when(buyService).buy(55L);

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String view = result.getResponseBody();
                    assertTrue(view.contains("DB Ошибка"));
                });
    }

    /**
     * Ошибка получения заказов — глобальная ошибка 500.
     */
    @Test
    @DisplayName("POST /buy — ошибка при получении заказов")
    void buyOrdersFetchErrorTest() {
        CartDto cartDto = new CartDto(99L, List.of(), BigDecimal.ONE);
        when(cartService.getItemsInTheCart()).thenReturn(Mono.just(cartDto));
        doNothing().when(buyService).buy(99L);
        when(orderService.getOrders()).thenReturn(Flux.error(new RuntimeException("Failure")));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String view = result.getResponseBody();
                    assertTrue(view.contains("Ошибка 500 — Внутренняя ошибка сервера"));
                });
    }
}
