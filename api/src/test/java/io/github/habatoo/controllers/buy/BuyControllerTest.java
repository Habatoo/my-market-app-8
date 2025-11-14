package io.github.habatoo.controllers.buy;

import io.github.habatoo.controllers.BuyController;
import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.servicies.BuyService;
import io.github.habatoo.servicies.CartService;
import io.github.habatoo.servicies.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для BuyController с использованием Mockito.
 * Проверяет обработку сценариев успешной покупки (создаётся новый заказ) и покупки без заказов.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для BuyController")
class BuyControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private CartService cartService;

    @Mock
    private BuyService buyService;

    @InjectMocks
    private BuyController buyController;

    /**
     * Тест кейс: успешное оформление покупки — должен произойти редирект на последний заказ с флагом newOrder.
     */
    @Test
    @DisplayName("Успешная покупка — редирект на новый заказ с флагом newOrder")
    void testBuyWithNewOrderTest() {
        CartDto cart = CartDto.builder().id(1L).build();
        OrderDto order1 = OrderDto.builder().id(10L).dateTime(LocalDateTime.now().minusMinutes(5)).build();
        OrderDto order2 = OrderDto.builder().id(11L).dateTime(LocalDateTime.now()).build();

        when(cartService.getItemsInTheCart()).thenReturn(cart);
        doNothing().when(buyService).buy(1L);
        when(orderService.getOrders()).thenReturn(List.of(order1, order2));

        String result = buyController.buy();

        assertEquals("redirect:/orders/11?newOrder=true", result);
        verify(buyService).buy(1L);
        verify(orderService).getOrders();
        verify(cartService).getItemsInTheCart();
    }

    /**
     * Тест кейс: корзина куплена, но новых заказов нет — должен произойти редирект на страницу заказов.
     */
    @Test
    @DisplayName("Покупка без заказов — редирект на список заказов")
    void testBuyNoOrdersTest() {
        CartDto cart = CartDto.builder().id(2L).build();

        when(cartService.getItemsInTheCart()).thenReturn(cart);
        doNothing().when(buyService).buy(2L);
        when(orderService.getOrders()).thenReturn(List.of());

        String result = buyController.buy();

        assertEquals("redirect:/orders/", result);
        verify(buyService).buy(2L);
        verify(orderService).getOrders();
        verify(cartService).getItemsInTheCart();
    }
}
