package io.github.habatoo.controllers;

import io.github.habatoo.dto.enums.Action;
import io.github.habatoo.dto.enums.Sort;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.request.GetItemsRequestDto;
import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.dto.response.ItemDtoResponse;
import io.github.habatoo.dto.response.ItemsDtoResponse;
import io.github.habatoo.servicies.CartService;
import io.github.habatoo.servicies.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private static final String ITEMS = "items";

    private static final String ITEM = "item";

    private final ItemService itemService;

    private final CartService cartService;

    @GetMapping
    public String getItems(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sort", required = false) Sort sort,
            @RequestParam(value = "pageSize", required = false, defaultValue = "5") Integer pageSize,
            @RequestParam(value = "pageNumber", required = false, defaultValue = "1") Integer pageNumber,
            Model model) {
        GetItemsRequestDto req = GetItemsRequestDto.builder()
                .search(search)
                .sort(sort != null ? sort : Sort.NO)
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .build();
        ItemsDtoResponse items = itemService.getItems(req);

        model.addAttribute("cart", items.cart());
        model.addAttribute(ITEMS, items.itemsRows());
        model.addAttribute("search", search == null ? "" : search);
        model.addAttribute("sort", req.getSort());
        model.addAttribute("paging", items.paging());

        return ITEMS;
    }

    @PostMapping
    public String changeNumberOfItems(
            @RequestParam("id") Long id,
            @RequestParam("action") String action,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sort", required = false) Sort sort,
            @RequestParam(value = "pageSize", required = false, defaultValue = "5") Integer pageSize,
            @RequestParam(value = "pageNumber", required = false, defaultValue = "1") Integer pageNumber) {
        ChangeNumberOfItemsRequestDto req = ChangeNumberOfItemsRequestDto.builder()
                .id(id)
                .action(Action.valueOf(action))
                .build();
        cartService.changeNumberOfItems(req);

        return "redirect:/items?search=" + (search == null ? "" : search)
                + "&sort=" + (sort == null ? "NO" : sort)
                + "&pageSize=" + pageSize
                + "&pageNumber=" + pageNumber;
    }

    @GetMapping("/{id}")
    public String getItemPage(
            @PathVariable("id") Long id,
            Model model) {
        ItemDtoResponse item = itemService.getItem(id);
        model.addAttribute(ITEM, item.item());
        model.addAttribute("cartCount", item.cartCount());

        return ITEM;
    }

    @PostMapping("/{id}")
    public String changeItemFromItemPage(
            @PathVariable("id") Long id,
            @RequestParam("action") String action,
            Model model) {
        ChangeNumberOfItemsRequestDto req = ChangeNumberOfItemsRequestDto.builder()
                .id(id)
                .action(Action.valueOf(action))
                .build();
        ItemDtoResponse item = itemService.changeNumberOfItemsFromPage(req);
        model.addAttribute(ITEM, item.item());
        model.addAttribute("cartCount", item.cartCount());

        return ITEM;
    }
}
