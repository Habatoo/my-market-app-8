package io.github.habatoo.controllers.order;

import io.github.habatoo.controllers.OrderController;
import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.servicies.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Unit-тесты для OrderController с использованием Mockito Extension.
 * Покрывает работу методов отображения списка заказов и отдельного заказа с разным состоянием newOrder.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для OrderController")
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private Model model;

    @InjectMocks
    private OrderController orderController;

    /**
     * Тест отображения списка заказов пользователя.
     * Проверяет возврат шаблона, вызов сервиса и добавление списка заказов в модель.
     */
    @Test
    @DisplayName("GET \"/orders\" — отображение списка заказов пользователя")
    void getOrderListTest() {
        Flux<OrderDto> orders =
                Flux.fromIterable(List.of(mock(OrderDto.class), mock(OrderDto.class)));

        when(orderService.getOrders()).thenReturn(orders);

        Mono<String> result = orderController.getOrderList(model);

        StepVerifier.create(result)
                .expectNext("orders")
                .verifyComplete();

        verify(orderService).getOrders();
    }

    /**
     * Тест отображения страницы заказа с флагом нового заказа (newOrder=true).
     * Проверяется корректная передача данных и установка признаков в модель.
     */
    @Test
    @DisplayName("GET \"/orders/{id}?order=true\" — отображение заказа с меткой нового")
    void getOrderNewOrderTrueTest() {
        Long id = 42L;
        boolean newOrder = true;
        Mono<OrderDto> dto = Mono.just(new OrderDto(id, List.of(), BigDecimal.ONE, LocalDateTime.now()));
        when(orderService.getOrder(id, newOrder)).thenReturn(dto);

        Mono<String> result = orderController.getOrder(id, newOrder, model);

        StepVerifier.create(result)
                .expectNext("order")
                .verifyComplete();

        verify(orderService).getOrder(id, true);
        verify(model).addAttribute(eq("order"), any(OrderDto.class));
        verify(model).addAttribute("newOrder", true);
    }

    /**
     * Тест отображения страницы заказа без флага нового заказа (newOrder=false).
     * Проверяет корректность передачи атрибутов и состояния.
     */
    @Test
    @DisplayName("GET \"/orders/{id}?order=false\" — отображение заказа без метки нового")
    void getOrderNewOrderFalseTest() {
        Long id = 42L;
        boolean newOrder = false;
        Mono<OrderDto> dto = Mono.just(new OrderDto(id, List.of(), BigDecimal.ONE, LocalDateTime.now()));
        when(orderService.getOrder(id, newOrder)).thenReturn(dto);

        Mono<String> result = orderController.getOrder(id, newOrder, model);

        StepVerifier.create(result)
                .expectNext("order")
                .verifyComplete();

        verify(orderService).getOrder(id, false);
        verify(model).addAttribute(eq("order"), any(OrderDto.class));
        verify(model).addAttribute("newOrder", false);
    }
}
