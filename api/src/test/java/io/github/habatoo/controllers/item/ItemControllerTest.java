package io.github.habatoo.controllers.item;

import io.github.habatoo.controllers.ItemController;
import io.github.habatoo.dto.enums.Action;
import io.github.habatoo.dto.enums.Sort;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.request.GetItemsRequestDto;
import io.github.habatoo.dto.response.*;
import io.github.habatoo.servicies.CartService;
import io.github.habatoo.servicies.ItemService;
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
class ItemControllerTest {

    @Mock
    private ItemService itemService;

    @Mock
    private CartService cartService;

    @Mock
    private Model model;

    @InjectMocks
    private ItemController itemController;

    @Test
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

        String result = itemController.getItems("test", Sort.NO, 5, 1, model);

        assertEquals("items", result);
        verify(itemService).getItems(any(GetItemsRequestDto.class));
        verify(model).addAttribute("cart", itemsResp.cart());
        verify(model).addAttribute("items", itemsResp.itemsRows());
        verify(model).addAttribute("search", "test");
        verify(model).addAttribute("sort", Sort.NO);
        verify(model).addAttribute("paging", itemsResp.paging());
    }

    @Test
    void testChangeNumberOfItems() {
        Long id = 1L;
        String action = "PLUS";
        when(cartService.changeNumberOfItems(any(ChangeNumberOfItemsRequestDto.class))).thenReturn(mock(ItemDto.class));

        String result = itemController.changeNumberOfItems(
                id,
                action,
                "searchQuery",
                Sort.PRICE,
                10,
                2);

        assertEquals("redirect:/items?search=searchQuery&sort=PRICE&pageSize=10&pageNumber=2", result);
        verify(cartService).changeNumberOfItems(
                argThat(req -> req.getId().equals(id) && req.getAction() == Action.PLUS));
    }

    @Test
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

    @Test
    void testChangeItemFromItemPage() {
        Long id = 12L;
        String action = "MINUS";
        ItemDtoResponse item = mock(ItemDtoResponse.class);
        when(itemService.changeNumberOfItemsFromPage(any(ChangeNumberOfItemsRequestDto.class))).thenReturn(item);

        String result = itemController.changeItemFromItemPage(id, action, model);

        assertEquals("item", result);
        verify(itemService).changeNumberOfItemsFromPage(argThat(req ->
                req.getId().equals(id) && req.getAction() == Action.MINUS));
        verify(model).addAttribute("item", item.item());
        verify(model).addAttribute("cartCount", item.cartCount());
    }
}
