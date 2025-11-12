package io.github.habatoo.controllers;

import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.servicies.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты для OrderController с использованием @WebMvcTest.
 * Покрывают отображение списка заказов и отдельного заказа, а также обработку ошибок.
 */
@WebMvcTest(OrderController.class)
@DisplayName("Интеграционный тест OrderController")
class OrderControllerWebMvcIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderController orderController;

    @MockitoBean
    private OrderService orderService;

    /**
     * Тест отображения списка заказов.
     */
    @Test
    @DisplayName("GET /orders — успешный возврат списка заказов с корректной моделью и view")
    void getOrdersListSuccessTest() throws Exception {
        List<OrderDto> orders = List.of(
                new OrderDto(1L, List.of(), BigDecimal.valueOf(100), LocalDateTime.now()),
                new OrderDto(2L, List.of(), BigDecimal.valueOf(200), LocalDateTime.now())
        );
        when(orderService.getOrders()).thenReturn(orders);

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("orders", orders))
                .andExpect(view().name("orders"));
    }

    /**
     * Тест отображения отдельного заказа с флагом нового заказа.
     */
    @Test
    @DisplayName("GET /orders/{id}?order=true — успешный возврат заказа с флагом newOrder")
    void getOrderWithNewOrderFlagTest() throws Exception {
        OrderDto orderDto = new OrderDto(11L, List.of(), BigDecimal.valueOf(555), LocalDateTime.now());
        when(orderService.getOrder(eq(11L), eq(true))).thenReturn(orderDto);

        mockMvc.perform(get("/orders/11").param("newOrder", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("order", orderDto))
                .andExpect(model().attribute("newOrder", true))
                .andExpect(view().name("order"));
    }

    /**
     * Тест — заказ не найден (возвращается страница ошибки 500).
     */
    @Test
    @DisplayName("GET /orders/{id} — заказ не найден, отображается error/500")
    void getOrderNotFoundTest() throws Exception {
        when(orderService.getOrder(eq(404L), anyBoolean()))
                .thenThrow(new IllegalStateException("Заказ не найден"));

        mockMvc.perform(get("/orders/404"))
                .andExpect(status().is5xxServerError())
                .andExpect(view().name("error/500"))
                .andExpect(model().attributeExists("error"));
    }
}
