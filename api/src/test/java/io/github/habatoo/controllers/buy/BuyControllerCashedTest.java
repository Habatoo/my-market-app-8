package io.github.habatoo.controllers.buy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.habatoo.controllers.BuyController;
import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.handlers.GlobalExceptionHandler;
import io.github.habatoo.servicies.BuyService;
import io.github.habatoo.servicies.CartService;
import io.github.habatoo.servicies.OrderService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <h2>Тесты для BuyController c максимальным кешированием MockMvc</h2>
 *
 * <p>
 * Класс покрывает unit-тесты основных методов контроллера BuyController с использованием Standalone MockMvc.
 * MockMvc и тестовые данные инициализируются единожды в @BeforeAll для максимальной производительности.
 * Каждый тест проверяет корректность эндпоинтов, обработку ошибок и возврат ожидаемых ответов.
 * Тесты полностью изолированы от Spring-контекста — мокируется только сервисный слой CommentService.
 * </p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Тесты unit уровня методов контроллера CommentController с использованием Cached MockMvc.")
class BuyControllerCashedTest {

    private MockMvc mockMvc;
    private OrderService orderService;
    private BuyService buyService;
    private CartService cartService;
    private ObjectMapper objectMapper;

    @BeforeAll
    void setUpAll() {
        orderService = mock(OrderService.class);
        buyService = mock(BuyService.class);
        cartService = mock(CartService.class);
        cartService = mock(CartService.class);
        BuyController buyController = new BuyController(orderService, buyService, cartService);
        mockMvc = MockMvcBuilders.standaloneSetup(buyController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    /**
     * Тест успешной покупки: редирект на последнюю покупку пользователя
     */
    @Test
    @DisplayName("POST \"/buy\" - должен вернуть редирект на последний заказ с флагом newOrder")
    void buySuccessTest() throws Exception {
        CartDto cartDto = mock(CartDto.class);
        when(cartDto.id()).thenReturn(42L);
        when(cartService.getItemsInTheCart()).thenReturn(cartDto);

        OrderDto orderDto = mock(OrderDto.class);
        when(orderDto.id()).thenReturn(111L);
        when(orderDto.dateTime()).thenReturn(LocalDateTime.now());
        List<OrderDto> orders = List.of(orderDto);

        when(orderService.getOrders()).thenReturn(orders);

        mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/orders/111?newOrder=true"));

        verify(buyService).buy(42L);
        verify(orderService, atLeastOnce()).getOrders();
        verify(cartService, atLeastOnce()).getItemsInTheCart();
    }

    /**
     * Тест для случая, когда заказов нет — редирект на базовый адрес
     */
    @Test
    @DisplayName("POST \"/buy\" - если нет заказов должен вернуть базовый редирект")
    void buyNoOrderTest() throws Exception {
        CartDto cartDto = mock(CartDto.class);
        when(cartDto.id()).thenReturn(1L);
        when(cartService.getItemsInTheCart()).thenReturn(cartDto);
        when(orderService.getOrders()).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/orders/"));

        verify(buyService).buy(1L);
        verify(orderService).getOrders();
        verify(cartService).getItemsInTheCart();
    }
}
