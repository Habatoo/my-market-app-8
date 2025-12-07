package io.github.habatoo.controllers;

import io.github.habatoo.dto.enums.Action;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.dto.response.CartItemDto;
import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.handlers.GlobalExceptionHandler;
import io.github.habatoo.servicies.CartService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.AssertionsKt.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * Интеграционный тест для CartController с использованием @WebFluxTest.
 * Проверяет все основные и граничные сценарии изменения количества товаров в корзине через контроллер.
 */
@WebFluxTest(CartController.class)
@Import(GlobalExceptionHandler.class)
@ImportAutoConfiguration(ThymeleafAutoConfiguration.class)
@DisplayName("Интеграционный WebFlux тест CartController")
class CartControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CartService cartService;

    /**
     * Тест успешного отображения содержимого корзины.
     */
    @Test
    @DisplayName("GET /cart — корзина выводится c корректным model attribute и view")
    void showCartSuccessTest() {
        CartDto cartDto = mock(CartDto.class);
        when(cartService.getItemsInTheCart()).thenReturn(Mono.just(cartDto));
        when(cartService.canProcessPayment(any())).thenReturn(Mono.just(Boolean.TRUE));

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String view = result.getResponseBody();
                    assertTrue(view.contains("<title>Корзина</title>"));
                });
    }

    /**
     * Тест — сервис корзины выбрасывает ошибку, должна вернуться страница ошибки.
     */
    @Test
    @DisplayName("GET /cart — ошибка сервиса корзины, отображается error/500")
    void showCartServiceErrorTest() {
        when(cartService.getItemsInTheCart())
                .thenReturn(Mono.error(new IllegalStateException("Корзина не найдена")));

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String view = result.getResponseBody();
                    assertTrue(view.contains("Ошибка 500 — Внутренняя ошибка сервера"));
                    assertTrue(view.contains("Корзина не найдена"));
                });
    }

    /**
     * Тест — добавление нового товара в корзину.
     */
    @Test
    @DisplayName("POST /cart/items — успешное добавление нового товара")
    void addNewItemToCartTest() {
        ChangeNumberOfItemsRequestDto requestDto = ChangeNumberOfItemsRequestDto.builder()
                .id(7L)
                .action(Action.PLUS)
                .build();
        CartDto cartDto = new CartDto(1L, List.of(new CartItemDto(mock(ItemDto.class), 1, BigDecimal.ONE)), BigDecimal.TWO);

        when(cartService.changeNumberOfItemsFromCart(eq(requestDto))).thenReturn(Mono.just(cartDto));
        when(cartService.getItemsInTheCart()).thenReturn(
                Mono.just(cartDto)
        );
        when(cartService.canProcessPayment(any())).thenReturn(Mono.just(Boolean.TRUE));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("id", "7")
                        .queryParam("action", "PLUS")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = new String(result.getResponseBody());
                    assertTrue(body.contains("cart"));
                });
    }

    /**
     * Тест — уменьшение количества товара, товар не удаляется.
     */
    @Test
    @DisplayName("POST /cart/items — уменьшение количества товара, товар остаётся в корзине")
    void decreaseItemQuantityTest() {
        CartDto cartDto = new CartDto(1L, List.of(new CartItemDto(mock(ItemDto.class), 3, BigDecimal.TEN)), BigDecimal.TWO);
        ChangeNumberOfItemsRequestDto requestDto = ChangeNumberOfItemsRequestDto.builder()
                .id(8L)
                .action(Action.MINUS)
                .build();

        when(cartService.changeNumberOfItemsFromCart(eq(requestDto))).thenReturn(Mono.just(cartDto));
        when(cartService.getItemsInTheCart()).thenReturn(
                Mono.just(cartDto)
        );
        when(cartService.canProcessPayment(any())).thenReturn(Mono.just(Boolean.TRUE));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("id", "8")
                        .queryParam("action", "MINUS")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String view = result.getResponseBody();
                    assertTrue(view.contains("Купить"));
                });
    }

    /**
     * Тест — уменьшение количества товара до 0, он должен быть удалён из корзины.
     */
    @Test
    @DisplayName("POST /cart/items — товар удаляется при уменьшении до нуля")
    void deleteItemWhenQuantityZeroTest() {
        ChangeNumberOfItemsRequestDto requestDto = ChangeNumberOfItemsRequestDto.builder()
                .id(9L)
                .action(Action.MINUS)
                .build();

        when(cartService.changeNumberOfItemsFromCart(eq(requestDto)))
                .thenReturn(Mono.just(new CartDto(0L, List.of(), BigDecimal.ZERO)));
        when(cartService.getItemsInTheCart())
                .thenReturn(Mono.just(new CartDto(0L, List.of(), BigDecimal.ZERO)));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("id", "9")
                        .queryParam("action", "MINUS")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String view = result.getResponseBody();
                    assertNotNull(view);
                    assertFalse(view.contains("Купить"));
                });
    }

    /**
     * Тест — ошибка, когда товар не найден.
     * Ожидается страница "error/500" и статус 500.
     */
    @Test
    @DisplayName("POST /cart/items — ошибка, если товар не найден")
    void errorWhenItemNotFoundTest() {
        when(cartService.changeNumberOfItemsFromCart(any()))
                .thenReturn(Mono.error(new IllegalStateException("Товар с id=999 не найден")));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("id", "999")
                        .queryParam("action", "PLUS")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String view = result.getResponseBody();
                    assertTrue(view.contains("Ошибка 500 — Внутренняя ошибка сервера"));
                    assertTrue(view.contains("Товар с id=999 не найден"));
                });
    }
}
