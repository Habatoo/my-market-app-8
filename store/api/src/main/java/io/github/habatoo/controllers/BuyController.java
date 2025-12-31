package io.github.habatoo.controllers;

import io.github.habatoo.servicies.BuyService;
import io.github.habatoo.servicies.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

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

    private final CartService cartService;
    private final BuyService buyService;

    /**
     * Оформить покупку: из корзины формируется заказ,
     * осуществляется покупка, выполняется перенаправление на страницу заказа.
     *
     * @return redirect на страницу заказов с флагом нового заказа (если создан)
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public Mono<String> buy() {
        return cartService.getItemsInTheCart()
                .flatMap(cart ->
                        buyService.buy(cart.id())
                                .map(orderId -> REDIRECT_ORDERS + orderId + "?newOrder=true")
                )
                .onErrorMap(ex -> {
                    log.warn("Ошибка оформления заказа: {}", ex.getMessage());
                    return ex;
                })
                .defaultIfEmpty(REDIRECT_ORDERS);
    }
}
