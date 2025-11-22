package io.github.habatoo.controllers.order;

import io.github.habatoo.configurations.DisableViewResolverConfiguration;
import io.github.habatoo.controllers.OrderController;
import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.handlers.GlobalExceptionHandler;
import io.github.habatoo.servicies.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для OrderController.
 * Проверяется корректность отображения списка заказов и отдельного заказа пользователя.
 * Используется WebFluxTest для имитации HTTP-запросов и проверки атрибутов модели и шаблона.
 */
@WebFluxTest(OrderController.class)
@ContextConfiguration(classes = OrderController.class)
@Import({DisableViewResolverConfiguration.class, GlobalExceptionHandler.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Тесты unit-уровня методов OrderController с использованием WebFluxTest")
class OrderControllerCashedTest {

    private static final String ORDERS = "orders";
    private static final String ORDER = "order";

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OrderService orderService;

    /**
     * Тест отображения списка всех заказов пользователя (GET /orders).
     * Проверяет, что в модель попадёт список заказов и будет возвращён правильный view.
     */
    @Test
    @DisplayName("GET /orders — список заказов")
    void getOrderListTest() {
        OrderDto order1 = mock(OrderDto.class);
        OrderDto order2 = mock(OrderDto.class);
        Flux<OrderDto> ordersFlux = Flux.just(order1, order2);

        when(orderService.getOrders()).thenReturn(ordersFlux);

        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo(ORDERS);

        verify(orderService).getOrders();
    }

    /**
     * Тест отображения отдельного заказа по id (GET /orders/{id}).
     * Проверяет, что в модель попадает нужный заказ и флаг newOrder.
     */
    @Test
    @DisplayName("GET /orders/{id} — заказ найден")
    void getOrderTest() {
        Long id = 77L;
        boolean newOrder = true;

        OrderDto orderDto = OrderDto.builder().items(List.of()).build();

        when(orderService.getOrder(id, newOrder)).thenReturn(Mono.just(orderDto));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/orders/{id}")
                        .queryParam("newOrder", "true")
                        .build(id))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo(ORDER);

        verify(orderService).getOrder(id, newOrder);
    }

    @Test
    @DisplayName("GET /orders/{id} — заказ не найден → глобальный обработчик 404")
    void getOrderNotFound() {
        Long id = 999L;
        when(orderService.getOrder(anyLong(), anyBoolean())).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/orders/{id}", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assertNotNull(body);
                    assertTrue(body.contains("error/404"));
                });

        verify(orderService).getOrder(eq(id), eq(false));
    }
}
