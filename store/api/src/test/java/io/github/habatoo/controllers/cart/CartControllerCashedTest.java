package io.github.habatoo.controllers.cart;

import io.github.habatoo.configurations.DisableViewResolverConfiguration;
import io.github.habatoo.controllers.CartController;
import io.github.habatoo.dto.enums.Action;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.dto.response.CartItemDto;
import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.handlers.GlobalExceptionHandler;
import io.github.habatoo.servicies.CartService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

/**
 * Unit-тесты для CartController.
 * Покрывает методы отображения корзины и обработку изменения количества товаров.
 * Используется WebFluxTest для имитации HTTP-запросов к контроллеру и проверки корректности модели и view.
 */
@WebFluxTest(CartController.class)
@ContextConfiguration(classes = CartController.class)
@Import({DisableViewResolverConfiguration.class, GlobalExceptionHandler.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Тесты unit-уровня методов CartController с использованием WebFluxTest")
public class CartControllerCashedTest {

    private static final String CART = "cart";

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CartService cartService;

    /**
     * Тест отображения корзины пользователя через GET-запрос.
     * Проверяет, что контроллер возвращает нужный шаблон и правильный объект корзины.
     */
    @Test
    @WithMockUser
    @DisplayName("GET \"/cart/items\" — отображение корзины пользователя")
    void showCartTest() {
        CartDto cartDto = new CartDto(
                1L,
                List.of(
                        new CartItemDto(
                                new ItemDto(10L, "item", "desc", "img", BigDecimal.TEN, 1),
                                2,
                                BigDecimal.valueOf(20)
                        )
                ),
                BigDecimal.valueOf(20)
        );
        when(cartService.getItemsInTheCart()).thenReturn(Mono.just(cartDto));
        when(cartService.canProcessPayment(any())).thenReturn(Mono.just(Boolean.TRUE));

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("cart");

        verify(cartService).getItemsInTheCart();
    }

    /**
     * Тест обработки изменения количества товаров в корзине через POST-запрос.
     * Проверка передачи DTO и возврата правильного шаблона с обновлённой корзиной.
     */
    @Test
    @WithMockUser
    @DisplayName("POST /cart/items — корректное изменение количества и возврат view 'cart'")
    void testChangeNumberOfItemsFromCart() {
        ChangeNumberOfItemsRequestDto req = ChangeNumberOfItemsRequestDto.builder()
                .id(10L)
                .action(Action.PLUS)
                .build();

        CartDto updated = new CartDto(
                1L,
                List.of(new CartItemDto(
                        new ItemDto(10L, "item", "desc", "img", BigDecimal.TEN, 1),
                        3,
                        BigDecimal.valueOf(30)
                )),
                BigDecimal.valueOf(30)
        );
        when(cartService.changeNumberOfItemsFromCart(any())).thenReturn(Mono.just(updated));
        when(cartService.canProcessPayment(any())).thenReturn(Mono.just(Boolean.TRUE));

        webTestClient
                .mutateWith(csrf())
                .post()
                .uri("/cart/items")
                .bodyValue(req)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(result -> {
                    String body = new String(result.getResponseBody());
                    assertTrue(body.contains("cart"));
                });

        verify(cartService).changeNumberOfItemsFromCart(any(ChangeNumberOfItemsRequestDto.class));
    }

    /**
     * POST /cart/items — пустой результат ⇒ возвращает 404 view
     */
    @Test
    @WithMockUser
    @DisplayName("POST /cart/items — если элемент корзины не найден, возвращается view пустой cart")
    void testChangeNumberOfItemsNotFound() {
        ChangeNumberOfItemsRequestDto req = ChangeNumberOfItemsRequestDto.builder().build();
        when(cartService.changeNumberOfItemsFromCart(any()))
                .thenReturn(Mono.empty());

        webTestClient
                .mutateWith(csrf())
                .post()
                .uri("/cart/items")
                .bodyValue(req)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertTrue(body.contains("cart"), "Должна отображаться страница cart"));

        verify(cartService).changeNumberOfItemsFromCart(eq(req));
    }

    /**
     * Тест: неавторизованный пользователь пытается просмотреть корзину.
     * Ожидаем 401 Unauthorized (или 302, если настроен редирект на логин).
     */
    @Test
    @DisplayName("GET /cart/items — анонимный пользователь получает 401")
    void showCartUnauthorizedTest() {
        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isUnauthorized();

        verifyNoInteractions(cartService);
    }

    /**
     * Тест: неавторизованный пользователь пытается изменить количество.
     * Даже с валидным CSRF, отсутствие сессии должно блокировать запрос.
     */
    @Test
    @DisplayName("POST /cart/items — анонимный пользователь получает 401")
    void changeNumberOfItemsUnauthorizedTest() {
        ChangeNumberOfItemsRequestDto req = ChangeNumberOfItemsRequestDto.builder()
                .id(10L)
                .action(Action.PLUS)
                .build();

        webTestClient
                .mutateWith(csrf())
                .post()
                .uri("/cart/items")
                .bodyValue(req)
                .exchange()
                .expectStatus().isUnauthorized();

        verifyNoInteractions(cartService);
    }

    /**
     * Тест: проверка отсутствия CSRF токена.
     * Даже если пользователь авторизован, отсутствие CSRF в POST запросе должно вернуть 403.
     */
    @Test
    @WithMockUser
    @DisplayName("POST /cart/items — отсутствие CSRF возвращает 403 Forbidden")
    void changeNumberOfItemsNoCsrfTest() {
        ChangeNumberOfItemsRequestDto req = ChangeNumberOfItemsRequestDto.builder().build();

        webTestClient.post()
                .uri("/cart/items")
                .bodyValue(req)
                .exchange()
                .expectStatus().isForbidden();
    }
}
