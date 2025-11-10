package io.github.habatoo.controllers;

import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.servicies.BuyService;
import io.github.habatoo.servicies.CartService;
import io.github.habatoo.servicies.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционный тест BuyController (@WebMvcTest) — покрытие всех крайних кейсов.
 * Покрывает: успешную покупку (заказ создан), покупку без заказов, ошибки/исключения на каждом шаге.
 */
@WebMvcTest(BuyController.class)
@DisplayName("Интеграционный тест BuyController")
class BuyControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BuyController buyController;

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
    void buySuccessTest() throws Exception {
        CartDto cartDto = mock(CartDto.class);
        when(cartDto.id()).thenReturn(42L);
        when(cartService.getItemsInTheCart()).thenReturn(cartDto);
        doNothing().when(buyService).buy(42L);

        OrderDto lastOrder = mock(OrderDto.class);
        when(lastOrder.id()).thenReturn(111L);
        when(lastOrder.dateTime()).thenReturn(LocalDateTime.now());
        when(orderService.getOrders()).thenReturn(List.of(lastOrder));

        mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/orders/111?newOrder=true"));
    }

    /**
     * Покупка: корзина есть, buy, но заказ не создан — редирект на основную страницу заказов.
     */
    @Test
    @DisplayName("POST /buy — нет заказов, редирект на /orders/")
    void buyNoOrderTest() throws Exception {
        CartDto cartDto = mock(CartDto.class);
        when(cartDto.id()).thenReturn(15L);
        when(cartService.getItemsInTheCart()).thenReturn(cartDto);
        doNothing().when(buyService).buy(15L);
        when(orderService.getOrders()).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/orders/"));
    }

    /**
     * Ошибка получения корзины — обработка исключения, возврат страницы ошибки.
     */
    @Test
    @DisplayName("POST /buy — корзина не найдена, выбрасывается исключение")
    void buyCartErrorTest() throws Exception {
        when(cartService.getItemsInTheCart()).thenThrow(new IllegalStateException("Корзина не найдена"));

        mockMvc.perform(post("/buy"))
                .andExpect(status().is5xxServerError())
                .andExpect(model().attributeExists("error"))
                .andExpect(view().name("error/500"));
    }

    /**
     * Ошибка создания заказа — обработка исключения сервиса покупки.
     */
    @Test
    @DisplayName("POST /buy — ошибка БД при создании заказа")
    void buyServiceErrorTest() throws Exception {
        CartDto cartDto = mock(CartDto.class);
        when(cartDto.id()).thenReturn(55L);
        when(cartService.getItemsInTheCart()).thenReturn(cartDto);
        doThrow(new DataAccessResourceFailureException("DB Ошибка")).when(buyService).buy(55L);

        mockMvc.perform(post("/buy"))
                .andExpect(status().is5xxServerError())
                .andExpect(model().attributeExists("error"))
                .andExpect(view().name("error/db"));
    }

    /**
     * Ошибка получения заказов — глобальная ошибка 500.
     */
    @Test
    @DisplayName("POST /buy — ошибка при получении заказов")
    void buyOrdersFetchErrorTest() throws Exception {
        CartDto cartDto = mock(CartDto.class);
        when(cartDto.id()).thenReturn(99L);
        when(cartService.getItemsInTheCart()).thenReturn(cartDto);
        doNothing().when(buyService).buy(99L);
        when(orderService.getOrders()).thenThrow(new RuntimeException("Failure"));

        mockMvc.perform(post("/buy"))
                .andExpect(status().is5xxServerError())
                .andExpect(model().attributeExists("error"))
                .andExpect(view().name("error/500"));
    }
}
