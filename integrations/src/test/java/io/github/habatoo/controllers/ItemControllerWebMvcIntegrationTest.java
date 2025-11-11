package io.github.habatoo.controllers;

import io.github.habatoo.dto.enums.Sort;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.request.GetItemsRequestDto;
import io.github.habatoo.dto.response.*;
import io.github.habatoo.servicies.CartService;
import io.github.habatoo.servicies.ItemService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты для ItemController с использованием @WebMvcTest.
 * Покрыты сценарии: получение витрины товаров с фильтрами, успешное изменение количества (POST),
 * возврат отдельной карточки, изменение количества из карточки, ошибки сервисов.
 */
@WebMvcTest(ItemController.class)
@DisplayName("Интеграционный тест ItemController")
class ItemControllerWebMvcIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ItemController itemController;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private CartService cartService;

    /**
     * Получение витрины товаров с параметрами фильтрации.
     */
    @Test
    @DisplayName("GET /items — витрина товаров с фильтрами, корректный model и view")
    void getItemsListTest() throws Exception {
        GetItemsRequestDto req = GetItemsRequestDto.builder().search("test").sort(Sort.ALPHA).pageSize(10).pageNumber(2).build();
        ItemsDtoResponse itemsDto = ItemsDtoResponse.builder()
                .itemsRows(List.of(List.of(new ItemDto(1L, "A", "desc", "", BigDecimal.ONE, 1))))
                .cart(mock(CartDto.class)).paging(mock(Paging.class)).build();

        when(itemService.getItems(any(GetItemsRequestDto.class))).thenReturn(itemsDto);

        mockMvc.perform(get("/items")
                        .param("search", "test")
                        .param("sort", "ALPHA")
                        .param("pageSize", "10")
                        .param("pageNumber", "2"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("cart"))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attribute("search", "test"))
                .andExpect(model().attribute("sort", Sort.ALPHA))
                .andExpect(model().attributeExists("paging"))
                .andExpect(view().name("items"));
    }

    /**
     * Изменение количества товара — редирект на витрину с сохранением фильтров.
     */
    @Test
    @DisplayName("POST /items — изменение количества товара, редирект на витрину с фильтрами")
    void changeNumberOfItemsRedirectTest() throws Exception {
        when(cartService.changeNumberOfItems(any(ChangeNumberOfItemsRequestDto.class))).thenReturn(mock(ItemDto.class));

        mockMvc.perform(post("/items")
                        .param("id", "5")
                        .param("action", "PLUS")
                        .param("search", "abc")
                        .param("sort", "PRICE")
                        .param("pageSize", "7")
                        .param("pageNumber", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items?search=abc&sort=PRICE&pageSize=7&pageNumber=2"));
    }

    /**
     * Получение отдельной карточки товара.
     */
    @Test
    @DisplayName("GET /items/{id} — возврат отдельной карточки товара")
    void getItemPageSuccessTest() throws Exception {
        ItemDtoResponse resp = new ItemDtoResponse(mock(ItemDto.class), 33);
        when(itemService.getItem(77L)).thenReturn(resp);

        mockMvc.perform(get("/items/77"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("item"))
                .andExpect(model().attribute("cartCount", 33))
                .andExpect(view().name("item"));
    }

    /**
     * Изменение количества из страницы карточки.
     */
    @Test
    @DisplayName("POST /items/{id} — изменение количества из карточки, возврат её же")
    void changeItemFromItemPageTest() throws Exception {
        ItemDtoResponse resp = new ItemDtoResponse(mock(ItemDto.class), 17);
        when(itemService.changeNumberOfItemsFromPage(any(ChangeNumberOfItemsRequestDto.class))).thenReturn(resp);

        mockMvc.perform(post("/items/88")
                        .param("action", "PLUS"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("item"))
                .andExpect(model().attribute("cartCount", 17))
                .andExpect(view().name("item"));
    }

    /**
     * Ошибка при получении отдельного товара (IllegalStateException).
     */
    @Test
    @DisplayName("GET /items/{id} — ошибка сервиса, отображается error/500")
    void getItemPageErrorTest() throws Exception {
        when(itemService.getItem(404L)).thenThrow(new IllegalStateException("Товар не найден"));

        mockMvc.perform(get("/items/404"))
                .andExpect(status().is5xxServerError())
                .andExpect(view().name("error/500"))
                .andExpect(model().attributeExists("error"));
    }
}
