package io.github.habatoo.controllers.cart;

import io.github.habatoo.controllers.CartController;
import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.servicies.CartService;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Unit-тесты для CartController.
 * Покрывает методы отображения корзины и обработку изменения количества товаров.
 * Используется MockMvc для имитации HTTP-запросов к контроллеру и проверки корректности модели и view.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Тесты unit-уровня методов CartController с использованием MockMvc")
public class CartControllerCashedTest {

    private static final String CART = "cart";
    private MockMvc mockMvc;
    private CartService cartService;

    @BeforeAll
    void setUpAll() {
        cartService = mock(CartService.class);
        CartController cartController = new CartController(cartService);
        mockMvc = MockMvcBuilders.standaloneSetup(cartController).build();
    }

    @BeforeEach
    void setUp() {
        reset(cartService);
    }

    /**
     * Тест отображения корзины пользователя через GET-запрос.
     * Проверяет, что контроллер возвращает нужный шаблон и правильный объект корзины.
     */
    @Test
    @DisplayName("GET \"/cart/items\" — отображение корзины пользователя")
    void showCartTest() throws Exception {
        CartDto cartDto = mock(CartDto.class);
        when(cartService.getItemsInTheCart()).thenReturn(cartDto);

        mockMvc.perform(get("/cart/items"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.model().attribute(CART, cartDto))
                .andExpect(MockMvcResultMatchers.view().name(CART));

        verify(cartService).getItemsInTheCart();
    }

    /**
     * Тест обработки изменения количества товаров в корзине через POST-запрос.
     * Проверка передачи DTO и возврата правильного шаблона с обновлённой корзиной.
     */
    @Test
    @DisplayName("POST \"/cart/items\" — изменение количества товаров в корзине")
    void changeNumberOfItemsFromCartTest() throws Exception {
        CartDto updatedCart = mock(CartDto.class);
        when(cartService.changeNumberOfItemsFromCart(any())).thenReturn(updatedCart);

        mockMvc.perform(post("/cart/items")
                        .param("id", "33")
                        .param("action", "PLUS"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.model().attribute(CART, updatedCart))
                .andExpect(MockMvcResultMatchers.view().name(CART));

        verify(cartService).changeNumberOfItemsFromCart(
                argThat(dto -> dto.getId().equals(33L) && "PLUS".equals(dto.getAction().name()))
        );
    }
}
