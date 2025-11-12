package io.github.habatoo.controllers.item;

import io.github.habatoo.controllers.ItemController;
import io.github.habatoo.dto.enums.Sort;
import io.github.habatoo.dto.request.GetItemsRequestDto;
import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.dto.response.ItemDtoResponse;
import io.github.habatoo.dto.response.ItemsDtoResponse;
import io.github.habatoo.servicies.CartService;
import io.github.habatoo.servicies.ItemService;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit-тесты для ItemController.
 * Проверяет обработку фильтрации, пагинации, изменение количества товара в корзине и отображение позиции товара.
 * Использует MockMvc для имитации HTTP-запросов и проверки модели/шаблонов.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Unit-тесты методов ItemController с использованием MockMvc")
class ItemControllerCashedTest {

    private static final String ITEMS = "items";
    private static final String ITEM = "item";
    private MockMvc mockMvc;
    private ItemService itemService;
    private CartService cartService;

    /**
     * Инициализация моков и MockMvc для всех тестов.
     * Настраивает мок-сервисы и подключает контроллер для тестирования.
     */
    @BeforeAll
    void setUpAll() {
        itemService = mock(ItemService.class);
        cartService = mock(CartService.class);
        ItemController itemController = new ItemController(itemService, cartService);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(itemController)
                .setViewResolvers(viewResolver)
                .build();
    }

    @BeforeEach
    void setUp() {
        reset(itemService);
        reset(cartService);
    }

    /**
     * Тест отображения витрины товаров с фильтрацией/пагинацией и корзиной.
     * Проверяет возврат шаблона и атрибутов модели (товары, корзина, поисковые параметры).
     */
    @Test
    @DisplayName("GET \"/items\" — отображение витрины с параметрами поиска и пагинации")
    void getItemsTest() throws Exception {
        ItemsDtoResponse itemsDtoResponse = ItemsDtoResponse.builder()
                .itemsRows(List.of(List.of()))
                .build();
        when(itemService.getItems(any())).thenReturn(itemsDtoResponse);

        mockMvc.perform(get("/items")
                        .param("search", "test")
                        .param("sort", "NO")
                        .param("pageSize", "5")
                        .param("pageNumber", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists(ITEMS))
                .andExpect(model().attributeExists("search"))
                .andExpect(model().attributeExists("sort"))
                .andExpect(view().name(ITEMS));

        verify(itemService).getItems(any());
    }

    /**
     * Тест обработки изменения количества товара в корзине и редиректа на витрину.
     * Проверяется вызов сервиса и корректный редирект с сохранением фильтров.
     */
    @Test
    @DisplayName("POST \"/items\" — изменение количества товара и редирект с фильтрами")
    void changeNumberOfItemsTest() throws Exception {
        when(cartService.changeNumberOfItems(any())).thenReturn(mock(ItemDto.class));

        mockMvc.perform(post("/items")
                        .param("id", "15")
                        .param("action", "PLUS")
                        .param("search", "test")
                        .param("sort", "NO")
                        .param("pageSize", "5")
                        .param("pageNumber", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items?search=test&sort=NO&pageSize=5&pageNumber=1"));

        verify(cartService).changeNumberOfItems(any());
    }

    /**
     * Тест отображения отдельной позиции товара и количества в корзине.
     * Проверяет правильную передачу модели и ожидание нужного view.
     */
    @Test
    @DisplayName("GET \"/items/{id}\" — отображение карточки позиции товара")
    void getItemPageTest() throws Exception {
        ItemDtoResponse itemDtoResponse = ItemDtoResponse.builder()
                .cartCount(3)
                .build();

        when(itemService.getItem(anyLong())).thenReturn(itemDtoResponse);

        mockMvc.perform(get("/items/33"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("cartCount"))
                .andExpect(view().name(ITEM));

        verify(itemService).getItem(33L);
    }

    /**
     * Тест изменения количества товара из страницы позиции и возврата этой же страницы.
     * Проверяет передачу модели и вызов соответствующего метода сервиса.
     */
    @Test
    @DisplayName("POST \"/items/{id}\" — изменение количества товара на странице и возвращение позиции")
    void changeItemFromItemPageTest() throws Exception {
        ItemDtoResponse itemDtoResponse = mock(ItemDtoResponse.class);
        when(itemService.changeNumberOfItemsFromPage(any())).thenReturn(itemDtoResponse);

        mockMvc.perform(post("/items/33")
                        .param("action", "PLUS"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("cartCount"))
                .andExpect(view().name(ITEM));

        verify(itemService).changeNumberOfItemsFromPage(any());
    }
}
