package io.github.habatoo.controllers;

import io.github.habatoo.dto.enums.Action;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.servicies.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/cart/items")
@RequiredArgsConstructor
public class CartController {

    private static final String CART = "cart";

    private final CartService cartService;

    @GetMapping
    public String showCart(Model model) {
        CartDto cart = cartService.getItemsInTheCart();
        model.addAttribute(CART, cart);

        return CART;
    }

    @PostMapping
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
        model.addAttribute(CART, cart);

        return CART;
    }
}
