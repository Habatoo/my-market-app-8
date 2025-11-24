package io.github.habatoo.controllers;

import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.servicies.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/**
 * Контроллер для работы с корзиной покупателя.
 * Позволяет отобразить содержимое корзины и изменять количество товаров.
 */
@Slf4j
@Controller
@RequestMapping("/cart/items")
@RequiredArgsConstructor
public class CartController {

    private static final String CART = "cart";

    private final CartService cartService;

    /**
     * Отображает содержимое корзины пользователя.
     *
     * @param model модель для передачи атрибутов на страницу
     * @return имя шаблона страницы корзины
     */
    @GetMapping
    public Mono<String> showCart(Model model) {
        log.info("GET /cart/items — отображение корзины");

        return cartService.getItemsInTheCart()
                .doOnNext(c -> model.addAttribute(CART, c))
                .thenReturn(CART);
    }

    /**
     * Обрабатывает изменение количества товаров в корзине (добавление/удаление/уменьшение)
     * через @ModelAttribute для автоматического связвания параметров запроса с DTO.
     *
     * @param req   DTO-запрос с идентификатором товара и действием (PLUS/MINUS)
     * @param model модель для передачи обновлённой корзины
     * @return имя шаблона страницы корзины
     */
    @PostMapping
    public Mono<String> changeNumberOfItemsFromCart(
            @ModelAttribute ChangeNumberOfItemsRequestDto req,
            Model model) {
        log.info("POST /cart/items — изменение количества товара, request={}", req);

        return cartService.changeNumberOfItemsFromCart(req)
                .doOnNext(cart -> model.addAttribute(CART, cart))
                .thenReturn(CART);
    }
}
