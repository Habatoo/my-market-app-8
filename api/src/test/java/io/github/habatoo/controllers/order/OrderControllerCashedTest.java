package io.github.habatoo.controllers.order;

import io.github.habatoo.controllers.OrderController;
import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.servicies.OrderService;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit-тесты для OrderController.
 * Проверяется корректность отображения списка заказов и отдельного заказа пользователя.
 * Используется MockMvc для имитации HTTP-запросов и проверки атрибутов модели и шаблона.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Тесты unit уровня методов контроллера OrderController с использованием MockMvc")
class OrderControllerCashedTest {

    private static final String ORDERS = "orders";
    private static final String ORDER = "order";
    private MockMvc mockMvc;
    private OrderService orderService;

    @BeforeAll
    void setUpAll() {
        orderService = mock(OrderService.class);
        OrderController orderController = new OrderController(orderService);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setViewResolvers(viewResolver)
                .build();
    }

    @BeforeEach
    void setUp() {
        reset(orderService);
    }

    /**
     * Тест отображения списка всех заказов пользователя (GET /orders).
     * Проверяет, что в модель попадёт список заказов и будет возвращён правильный view.
     */
    @Test
    @DisplayName("GET \"/orders\" — отображение списка заказов пользователя")
    void getOrderListTest() throws Exception {
        List<OrderDto> orders = List.of(mock(OrderDto.class), mock(OrderDto.class));
        when(orderService.getOrders()).thenReturn(orders);

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(model().attribute(ORDERS, orders))
                .andExpect(view().name(ORDERS));

        verify(orderService).getOrders();
    }

    /**
     * Тест отображения отдельного заказа по id (GET /orders/{id}).
     * Проверяет, что в модель попадает нужный заказ и флаг newOrder.
     */
    @Test
    @DisplayName("GET \"/orders/{id}\" — отображение страницы отдельного заказа")
    void getOrderTest() throws Exception {
        Long id = 77L;
        boolean newOrder = true;
        OrderDto orderDto = OrderDto.builder()
                .items(List.of())
                .build();
        when(orderService.getOrder(id, newOrder)).thenReturn(orderDto);

        mockMvc.perform(get("/orders/{id}", id)
                        .param("newOrder", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attribute(ORDER, orderDto))
                .andExpect(model().attribute("newOrder", true))
                .andExpect(view().name(ORDER));

        verify(orderService).getOrder(id, newOrder);
    }
}
