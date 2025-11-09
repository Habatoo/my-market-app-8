package io.github.habatoo.controllers;

import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.servicies.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final String ORDERS = "orders";

    private static final String ORDER = "order";

    private final OrderService orderService;

    @GetMapping
    public String getOrderList(Model model) {
        List<OrderDto> orders = orderService.getOrders();
        model.addAttribute(ORDERS, orders);

        return ORDERS;
    }

    @GetMapping("/{id}")
    public String getOrder(
            @PathVariable Long id,
            @RequestParam(value = ORDER, required = false, defaultValue = "false") boolean newOrder,
            Model model) {
        model.addAttribute(ORDER, orderService.getOrder(id, newOrder));
        model.addAttribute("newOrder", newOrder);

        return ORDER;
    }
}
