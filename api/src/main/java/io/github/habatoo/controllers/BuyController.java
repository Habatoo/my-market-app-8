package io.github.habatoo.controllers;

import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.servicies.BuyService;
import io.github.habatoo.servicies.CartService;
import io.github.habatoo.servicies.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Comparator;
import java.util.Optional;

/**
 * Контроллер, отвечающий за оформление покупки товара из корзины пользователем.
 * Логика: оформление заказа, получение нового заказа и перенаправление на страницу заказов.
 */
@Slf4j
@Controller
@RequestMapping("/buy")
@RequiredArgsConstructor
public class BuyController {

    private static final String REDIRECT_ORDERS = "redirect:/orders/";

    private final OrderService orderService;
    private final BuyService buyService;
    private final CartService cartService;

    /**
     * Оформить покупку: из корзины формируется заказ,
     * осуществляется покупка, выполняется перенаправление на страницу заказа.
     *
     * @return redirect на страницу заказов с флагом нового заказа (если создан)
     */
    @PostMapping
    public String buy() {
        log.info("POST /buy — старт процедуры покупки");

        CartDto cart = cartService.getItemsInTheCart();
        log.debug("Корзина для покупки: cartId={}, itemsCount={}", cart.id(), cart.items().size());

        buyService.buy(cart.id());
        log.info("Покупка совершена для корзины id={}", cart.id());

        Optional<OrderDto> latestOrder = orderService.getOrders().stream()
                .max(Comparator.comparing(OrderDto::dateTime));
        String redirectUrl = latestOrder
                .map(order -> REDIRECT_ORDERS + order.id() + "?newOrder=true")
                .orElse(REDIRECT_ORDERS);
        log.info("Редирект после покупки: {}", redirectUrl);

        return redirectUrl;
    }
}
