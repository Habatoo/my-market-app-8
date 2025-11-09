package io.github.habatoo.controllers;

import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.servicies.BuyService;
import io.github.habatoo.servicies.CartService;
import io.github.habatoo.servicies.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Comparator;

@Controller
@RequestMapping("/buy")
@RequiredArgsConstructor
public class BuyController {

    private static final String REDIRECT_ORDERS = "redirect:/orders/";

    private final OrderService orderService;
    private final BuyService buyService;
    private final CartService cartService;

    @PostMapping
    public String buy() {
        CartDto cart = cartService.getItemsInTheCart();
        buyService.buy(cart.id());

        return orderService.getOrders().stream()
                .max(Comparator.comparing(OrderDto::dateTime))
                .map(order -> REDIRECT_ORDERS + order.id() + "?newOrder=true")
                .orElse(REDIRECT_ORDERS);
    }
}
