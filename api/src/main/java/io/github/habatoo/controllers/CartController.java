package io.github.habatoo.controllers;

import io.github.habatoo.dto.enums.Action;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.enums.Sort;
import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.servicies.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/cart/items")
    public String showCart(Model model) {
        CartDto cart = cartService.getItemsInTheCart();
        model.addAttribute("cart", cart);

        return "cart";
    }

    @PostMapping("/items")
    public String changeNumberOfItems(
            @RequestParam("id") Long id,
            @RequestParam("action") String action,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sort", required = false) Sort sort,
            @RequestParam(value = "pageSize", required = false, defaultValue = "5") Integer pageSize,
            @RequestParam(value = "pageNumber", required = false, defaultValue = "1") Integer pageNumber
    ) {
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

    @PostMapping("/cart/items")
    public String changeNumberOfItemsFromCart(
            @RequestParam("id") Long id,
            @RequestParam("action") String action,
            Model model
    ) {
        ChangeNumberOfItemsRequestDto req = ChangeNumberOfItemsRequestDto.builder()
                .id(id)
                .action(Action.valueOf(action))
                .build();
        CartDto cart = cartService.changeNumberOfItemsFromCart(req);
        model.addAttribute("cart", cart);

        return "cart";
    }
}
