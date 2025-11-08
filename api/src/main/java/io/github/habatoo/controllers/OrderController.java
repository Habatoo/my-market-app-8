package io.github.habatoo.controllers;

import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.servicies.CartService;
import io.github.habatoo.servicies.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    private final CartService cartService;

    @GetMapping("/orders")
    public String getOrderList(Model model) {
        model.addAttribute("orders", orderService.getOrders());

        return "orders";
    }

    @GetMapping("/orders/{id}")
    public String getOrder(
            @PathVariable Long id,
            @RequestParam(value = "newOrder", required = false, defaultValue = "false") boolean newOrder,
            Model model) {
        model.addAttribute("order", orderService.getOrder(id, newOrder));
        model.addAttribute("newOrder", newOrder);

        return "order";
    }

    @PostMapping("/buy")
    public String buy() {
        CartDto cart = cartService.getItemsInTheCart();
        orderService.buy(cart.id());

        List<OrderDto> orders = orderService.getOrders();
        OrderDto newOrder = orders.stream()
                .max(Comparator.comparing(OrderDto::dateTime))
                .orElse(null);

        if (newOrder != null) {
            return "redirect:/orders/" + newOrder.id() + "?newOrder=true";
        }
        return "redirect:/orders";
    }
}
