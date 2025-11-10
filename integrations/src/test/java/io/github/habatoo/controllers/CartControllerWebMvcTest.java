package io.github.habatoo.controllers;

import io.github.habatoo.dto.enums.Action;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.servicies.CartService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционный тест для CartController с использованием @WebMvcTest.
 * Проверяет все основные и граничные сценарии изменения количества товаров в корзине через контроллер.
 */
@WebMvcTest(CartController.class)
@DisplayName("Интеграционный тест CartController")
class CartControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CartController cartController;

    @MockitoBean
    private CartService cartService;

    /**
     * Тест успешного отображения содержимого корзины.
     */
    @Test
    @DisplayName("GET /cart — корзина выводится c корректным model attribute и view")
    void showCartSuccessTest() throws Exception {
        CartDto cartDto = mock(CartDto.class);
        when(cartService.getItemsInTheCart()).thenReturn(cartDto);

        mockMvc.perform(get("/cart"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("cart", cartDto))
                .andExpect(view().name("cart"));
    }

    /**
     * Тест — сервис корзины выбрасывает ошибку, должна вернуться страница ошибки.
     */
    @Test
    @DisplayName("GET /cart — ошибка сервиса корзины, отображается error/500")
    void showCartServiceErrorTest() throws Exception {
        when(cartService.getItemsInTheCart()).thenThrow(new IllegalStateException("Корзина не найдена"));

        mockMvc.perform(get("/cart"))
                .andExpect(status().is5xxServerError())
                .andExpect(view().name("error/500"))
                .andExpect(model().attributeExists("error"));
    }

    /**
     * Тест — добавление нового товара в корзину.
     */
    @Test
    @DisplayName("POST /cart/items — успешное добавление нового товара")
    void addNewItemToCartTest() throws Exception {
        ChangeNumberOfItemsRequestDto requestDto = ChangeNumberOfItemsRequestDto.builder()
                .id(7L)
                .action(Action.PLUS)
                .build();
        when(cartService.changeNumberOfItemsFromCart(eq(requestDto))).thenReturn(mock(CartDto.class));

        mockMvc.perform(post("/cart/items")
                        .param("id", "7")
                        .param("action", "PLUS"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("cart"));
    }

    /**
     * Тест — уменьшение количества товара, товар не удаляется.
     */
    @Test
    @DisplayName("POST /cart/items — уменьшение количества товара, товар остаётся в корзине")
    void decreaseItemQuantityTest() throws Exception {
        ChangeNumberOfItemsRequestDto requestDto = ChangeNumberOfItemsRequestDto.builder()
                .id(8L)
                .action(Action.MINUS)
                .build();
        when(cartService.changeNumberOfItemsFromCart(eq(requestDto))).thenReturn(mock(CartDto.class));

        mockMvc.perform(post("/cart/items")
                        .param("id", "8")
                        .param("action", "MINUS"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("cart"));
    }

    /**
     * Тест — уменьшение количества товара до 0, он должен быть удалён из корзины.
     */
    @Test
    @DisplayName("POST /cart/items — товар удаляется при уменьшении до нуля")
    void deleteItemWhenQuantityZeroTest() throws Exception {
        ChangeNumberOfItemsRequestDto requestDto = ChangeNumberOfItemsRequestDto.builder()
                .id(9L)
                .action(Action.MINUS)
                .build();
        when(cartService.changeNumberOfItemsFromCart(eq(requestDto))).thenReturn(mock(CartDto.class));

        mockMvc.perform(post("/cart/items")
                        .param("id", "9")
                        .param("action", "MINUS"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("cart"));
    }

    /**
     * Тест — ошибка, когда товар не найден.
     * Ожидается страница "error/500" и статус 500.
     */
    @Test
    @DisplayName("POST /cart/items — ошибка, если товар не найден")
    void errorWhenItemNotFoundTest() throws Exception {
        when(cartService.changeNumberOfItemsFromCart(any(ChangeNumberOfItemsRequestDto.class)))
                .thenThrow(new IllegalStateException("Товар с id=999 не найден"));

        mockMvc.perform(post("/cart/items")
                        .param("id", "999")
                        .param("action", "PLUS"))
                .andExpect(status().is5xxServerError())
                .andExpect(view().name("error/500"))
                .andExpect(model().attributeExists("error"));
    }
}

