package io.github.habatoo.controllers.order;

import io.github.habatoo.controllers.OrderController;
import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.servicies.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private Model model;

    @InjectMocks
    private OrderController orderController;

    @Test
    void getOrderListTest() {
        List<OrderDto> orders = List.of(mock(OrderDto.class), mock(OrderDto.class));
        when(orderService.getOrders()).thenReturn(orders);

        String result = orderController.getOrderList(model);

        assertEquals("orders", result);
        verify(orderService).getOrders();
        verify(model).addAttribute("orders", orders);
    }

    @Test
    void getOrderNewOrderTrueTest() {
        Long id = 42L;
        boolean newOrder = true;
        OrderDto dto = mock(OrderDto.class);
        when(orderService.getOrder(id, newOrder)).thenReturn(dto);

        String result = orderController.getOrder(id, newOrder, model);

        assertEquals("order", result);
        verify(orderService).getOrder(id, true);
        verify(model).addAttribute("order", dto);
        verify(model).addAttribute("newOrder", true);
    }

    @Test
    void getOrderNewOrderFalseTest() {
        Long id = 42L;
        boolean newOrder = false;
        OrderDto dto = mock(OrderDto.class);
        when(orderService.getOrder(id, newOrder)).thenReturn(dto);

        String result = orderController.getOrder(id, newOrder, model);

        assertEquals("order", result);
        verify(orderService).getOrder(id, false);
        verify(model).addAttribute("order", dto);
        verify(model).addAttribute("newOrder", false);
    }
}
