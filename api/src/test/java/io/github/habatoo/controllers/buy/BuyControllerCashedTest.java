package io.github.habatoo.controllers.buy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.habatoo.controllers.BuyController;
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

import static org.mockito.Mockito.*;

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
     * Тест успешной покупки
     */
    @Test
    @DisplayName("POST \"/buy\" - должен вернуть пустой список")
    void buySuccessTest() throws Exception {
    }
}
