package io.github.habatoo.controllers.cart;

import io.github.habatoo.controllers.CartController;
import io.github.habatoo.dto.enums.Action;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.servicies.CartService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Unit-тесты для CartController с использованием Mockito Extension.
 * Проверяет обработку отображения корзины и изменение количества товаров в корзине.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для CartController")
class CartControllerTest {

    @Mock
    private CartService cartService;

    @Mock
    private Model model;

    @InjectMocks
    private CartController cartController;

    /**
     * Тест отображения корзины пользователя.
     * Проверяет вызов сервиса CartService и добавление корзины в модель,
     * а также корректное имя возвращаемого шаблона.
     */
    @Test
    @DisplayName("GET \"/cart/items\" — отображение корзины пользователя")
    void showCartTest() {
        Mono<CartDto> cart = Mono.just(new CartDto(1L, List.of(), BigDecimal.ONE));
        when(cartService.getItemsInTheCart()).thenReturn(cart);
        when(cartService.canProcessPayment(any())).thenReturn(Mono.just(Boolean.TRUE));

        Mono<String> result = cartController.showCart(model);

        StepVerifier.create(result)
                .expectNext("cart")
                .verifyComplete();

        verify(cartService).getItemsInTheCart();
        verify(model).addAttribute(eq("cart"), any(CartDto.class));
    }

    /**
     * Тест изменения количества товаров в корзине через DTO-запрос.
     * Проверяет корректность передачи DTO, вызова сервиса и добавления обновлённой корзины в модель,
     * а также верность имени шаблона корзины.
     */
    @Test
    @DisplayName("POST \"/cart/items\" — изменение количества товаров в корзине")
    void changeNumberOfItemsFromCartTest() {
        Long id = 51L;
        String action = "PLUS";
        Mono<CartDto> cart = Mono.just(new CartDto(1L, List.of(), BigDecimal.ONE));

        ChangeNumberOfItemsRequestDto req =
                ChangeNumberOfItemsRequestDto.builder()
                        .id(id)
                        .action(Action.valueOf(action))
                        .build();

        when(cartService.changeNumberOfItemsFromCart(any(ChangeNumberOfItemsRequestDto.class)))
                .thenReturn(cart);
        when(cartService.canProcessPayment(any())).thenReturn(Mono.just(Boolean.TRUE));

        Mono<String> result = cartController.changeNumberOfItemsFromCart(req, model);

        StepVerifier.create(result)
                .expectNext("cart")
                .verifyComplete();

        verify(cartService).changeNumberOfItemsFromCart(refEq(req));
        verify(model).addAttribute(eq("cart"), any(CartDto.class));
    }

    /**
     * GET /cart/items — если корзина не найдена, возвращается view "cart"
     * и модель не изменяется.
     */
    @Test
    @DisplayName("GET /cart/items — корзина не найдена, возвращается view cart без изменения модели")
    void showCartNotFoundTest() {
        when(cartService.getItemsInTheCart())
                .thenReturn(Mono.empty());

        Mono<String> result = cartController.showCart(model);

        StepVerifier.create(result)
                .expectNext("cart")
                .verifyComplete();

        verify(cartService).getItemsInTheCart();
        verify(model, never()).addAttribute(eq("cart"), any());
    }

    /**
     * POST /cart/items — если элемент корзины не найден, возвращается view "cart",
     * при этом модель не заполняется объектом Cart.
     */
    @Test
    @DisplayName("POST /cart/items — элемент корзины не найден, возвращается view cart без изменения модели")
    void changeNumberOfItemsNotFoundTest() {
        ChangeNumberOfItemsRequestDto req = ChangeNumberOfItemsRequestDto.builder().build();
        when(cartService.changeNumberOfItemsFromCart(any(ChangeNumberOfItemsRequestDto.class)))
                .thenReturn(Mono.empty());

        Mono<String> result = cartController.changeNumberOfItemsFromCart(req, model);

        StepVerifier.create(result)
                .expectNext("cart")
                .verifyComplete();

        verify(cartService).changeNumberOfItemsFromCart(refEq(req));
        verify(model, never()).addAttribute(eq("cart"), any());
    }

}
