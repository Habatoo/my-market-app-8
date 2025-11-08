package io.github.habatoo.controllers;

import io.github.habatoo.dto.enums.Action;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.request.GetItemsRequestDto;
import io.github.habatoo.dto.enums.Sort;
import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.dto.response.CartItemDto;
import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.dto.response.Paging;
import io.github.habatoo.servicies.CartService;
import io.github.habatoo.servicies.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    private final CartService cartService;

    @GetMapping(value = {"/", "/items"})
    public String getItems(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sort", required = false) Sort sort,
            @RequestParam(value = "pageNumber", required = false, defaultValue = "1") Integer pageNumber,
            @RequestParam(value = "pageSize", required = false, defaultValue = "5") Integer pageSize,
            Model model
    ) {
        GetItemsRequestDto req = GetItemsRequestDto.builder()
                .search(search)
                .sort(sort != null ? sort : Sort.NO)
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .build();

        List<ItemDto> items = itemService.getItems(req);
        CartDto cart = cartService.getItemsInTheCart();

        List<List<ItemDto>> itemsRows = splitByRows(items, 3);

        model.addAttribute("cart", cart);
        model.addAttribute("items", itemsRows);
        model.addAttribute("search", search == null ? "" : search);
        model.addAttribute("sort", req.getSort());
        model.addAttribute("paging", new Paging(items.size(), pageSize, pageNumber, pageNumber > 1, pageNumber * pageSize < items.size()));

        return "items";
    }

    @GetMapping("/items/{id}")
    public String getItemPage(@PathVariable("id") Long id, Model model) {
        ItemDto item = itemService.getItem(id);
        CartDto cart = cartService.getItemsInTheCart();
        int cartCount = cart.getCountByItemId(id);

        model.addAttribute("item", item);
        model.addAttribute("cartCount", cartCount);

        return "item";
    }

    @PostMapping("/items/{id}")
    public String changeItemFromItemPage(
            @PathVariable("id") Long id,
            @RequestParam("action") String action,
            Model model
    ) {
        ChangeNumberOfItemsRequestDto req = ChangeNumberOfItemsRequestDto.builder()
                .id(id)
                .action(Action.valueOf(action))
                .build();
        ItemDto item = itemService.changeNumberOfItemsFromPage(req);
        model.addAttribute("item", item);

        return "item";
    }

    private List<List<ItemDto>> splitByRows(List<ItemDto> items, int rowSize) {
        int totalRows = (int) Math.ceil((double) items.size() / rowSize);

        return IntStream.range(0, totalRows)
                .mapToObj(i -> items.subList(i * rowSize, Math.min(items.size(), (i + 1) * rowSize)))
                .map(subList -> {
                    List<ItemDto> l = new ArrayList<>(subList);
                    while (l.size() < rowSize) {
                        l.add(new ItemDto(-1L, "", "", "", null, 0)); // заглушка
                    }
                    return l;
                })
                .collect(Collectors.toList());
    }
}
