package io.github.habatoo.controllers.item;

import io.github.habatoo.controllers.ItemController;
import io.github.habatoo.dto.enums.Action;
import io.github.habatoo.dto.enums.Sort;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.request.GetItemsRequestDto;
import io.github.habatoo.dto.response.*;
import io.github.habatoo.servicies.CartService;
import io.github.habatoo.servicies.ItemService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для ItemController с использованием Mockito Extension.
 * Проверяется корректность работы методов отображения списка, изменения количества товара,
 * просмотра позиции и изменения количества из карточки товара.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для ItemController")
class ItemControllerTest {

    @Mock
    private ItemService itemService;

    @Mock
    private CartService cartService;

    @Mock
    private Model model;

    @InjectMocks
    private ItemController itemController;

    /**
     * Тест получения и отображения списка товаров с фильтрацией и пагинацией.
     * Проверяет добавление объектов в модель и возврат правильного имени шаблона.
     */
    @Test
    @DisplayName("GET \"/items\" — успешно возвращает витрину товаров с фильтрацией")
    void testGetItems() {
        GetItemsRequestDto req = GetItemsRequestDto.builder()
                .search("test")
                .sort(Sort.NO)
                .pageNumber(1)
                .pageSize(5)
                .build();
        ItemsDtoResponse itemsResp = mock(ItemsDtoResponse.class);
        when(itemService.getItems(any(GetItemsRequestDto.class))).thenReturn(itemsResp);
        when(itemsResp.cart()).thenReturn(mock(CartDto.class));
        when(itemsResp.itemsRows()).thenReturn(List.of());
        when(itemsResp.paging()).thenReturn(mock(Paging.class));

        String result = itemController.getItems(req, model);

        assertEquals("items", result);
        verify(itemService).getItems(any(GetItemsRequestDto.class));
        verify(model).addAttribute("cart", itemsResp.cart());
        verify(model).addAttribute("items", itemsResp.itemsRows());
        verify(model).addAttribute("search", "test");
        verify(model).addAttribute("sort", Sort.NO);
        verify(model).addAttribute("paging", itemsResp.paging());
    }

    /**
     * Тест изменения количества конкретного товара в корзине с редиректом на витрину и фильтрами.
     * Проверяет генерацию правильной ссылки и вызов сервиса изменения количеста.
     */
    @Test
    @DisplayName("POST \"/items\" — изменение количества товара, корректный редирект")
    void testChangeNumberOfItems() {
        Long id = 1L;
        ChangeNumberOfItemsRequestDto req = ChangeNumberOfItemsRequestDto.builder()
                .id(id)
                .search("searchQuery")
                .action(Action.PLUS)
                .sort(Sort.PRICE)
                .pageNumber(2)
                .pageSize(10)
                .build();
        when(cartService.changeNumberOfItems(eq(req))).thenReturn(mock(ItemDto.class));

        String result = itemController.changeNumberOfItems(req);

        assertEquals("redirect:/items?search=searchQuery&sort=PRICE&pageSize=10&pageNumber=2", result);
        verify(cartService).changeNumberOfItems(
                argThat(requestDto -> requestDto.getId().equals(id)
                        && requestDto.getAction() == Action.PLUS));
    }

    /**
     * Тест отображения отдельной карточки товара и количествав корзине.
     * Проверяет передачу в модель данных товара и количества.
     */
    @Test
    @DisplayName("GET \"/items/{id}\" — отображение карточки товара")
    void testGetItemPage() {
        Long id = 42L;
        ItemDtoResponse itemResp = mock(ItemDtoResponse.class);
        when(itemService.getItem(id)).thenReturn(itemResp);
        when(itemResp.item()).thenReturn(mock(ItemDto.class));
        when(itemResp.cartCount()).thenReturn(3);

        String result = itemController.getItemPage(id, model);

        assertEquals("item", result);
        verify(itemService).getItem(id);
        verify(model).addAttribute("item", itemResp.item());
        verify(model).addAttribute("cartCount", 3);
    }

    /**
     * Тест изменения количества товара из карточки товара и возврата свежей карточки.
     * Проверяет вызов метода сервиса и заполнение модели актуальными данными.
     */
    @Test
    @DisplayName("POST \"/items/{id}\" — изменение количества, возврат обновлённой карточки товара")
    void testChangeItemFromItemPage() {
        Long id = 12L;
        ChangeNumberOfItemsRequestDto req = ChangeNumberOfItemsRequestDto.builder()
                .id(id)
                .search("searchQuery")
                .action(Action.MINUS)
                .pageNumber(10)
                .pageSize(2)
                .build();
        ItemDtoResponse item = mock(ItemDtoResponse.class);
        when(itemService.changeNumberOfItemsFromPage(eq(req))).thenReturn(item);

        String result = itemController.changeItemFromItemPage(id, req, model);

        assertEquals("item", result);
        verify(itemService).changeNumberOfItemsFromPage(argThat(requestDto ->
                requestDto.getId().equals(id) && requestDto.getAction() == Action.MINUS));
        verify(model).addAttribute("item", item.item());
        verify(model).addAttribute("cartCount", item.cartCount());
    }
}
