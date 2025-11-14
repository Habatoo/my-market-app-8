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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        CartDto cart = mock(CartDto.class);
        when(cartService.getItemsInTheCart()).thenReturn(cart);

        String result = cartController.showCart(model);

        assertEquals("cart", result);
        verify(cartService).getItemsInTheCart();
        verify(model).addAttribute("cart", cart);
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
        CartDto cart = mock(CartDto.class);

        ChangeNumberOfItemsRequestDto req =
                ChangeNumberOfItemsRequestDto.builder()
                        .id(id)
                        .action(Action.valueOf(action))
                        .build();

        when(cartService.changeNumberOfItemsFromCart(any(ChangeNumberOfItemsRequestDto.class)))
                .thenReturn(cart);

        String result = cartController.changeNumberOfItemsFromCart(req, model);

        assertEquals("cart", result);
        verify(cartService).changeNumberOfItemsFromCart(refEq(req));
        verify(model).addAttribute("cart", cart);
    }
}
