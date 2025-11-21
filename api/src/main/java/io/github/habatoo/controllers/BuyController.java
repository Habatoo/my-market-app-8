package io.github.habatoo.controllers;

import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.servicies.BuyService;
import io.github.habatoo.servicies.CartService;
import io.github.habatoo.servicies.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

import java.util.Comparator;

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
    public Mono<String> buy() {
        log.info("POST /buy — старт процедуры покупки");

        return cartService.getItemsInTheCart()
                .doOnNext(cart -> log.debug("Корзина для покупки: cartId={}, itemsCount={}",
                        cart.id(), cart.items().size()))
                .flatMap(cart ->
                        Mono.fromRunnable(() -> buyService.buy(cart.id()))
                                .thenReturn(cart)
                                .doOnNext(c -> log.info("Покупка совершена для корзины id={}", c.id()))
                )
                .flatMap(cart ->
                        orderService.getOrders()
                                .sort(Comparator.comparing(OrderDto::dateTime))
                                .takeLast(1).singleOrEmpty()
                                .map(order -> REDIRECT_ORDERS + order.id() + "?newOrder=true")
                                .defaultIfEmpty(REDIRECT_ORDERS)
                )
                .doOnNext(redirect -> log.info("Редирект после покупки: {}", redirect));
    }
}
