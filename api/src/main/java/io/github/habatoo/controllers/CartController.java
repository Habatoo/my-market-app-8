package io.github.habatoo.controllers;

import io.github.habatoo.dto.enums.Action;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.servicies.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для работы с корзиной покупателя.
 * Позволяет отобразить содержимое корзины и изменять количество товаров.
 */
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
    public String showCart(Model model) {
        CartDto cart = cartService.getItemsInTheCart();
        model.addAttribute(CART, cart);

        return CART;
    }

    /**
     * Обрабатывает изменение количества товаров в корзине (добавление/удаление/уменьшение).
     *
     * @param id    идентификатор товара
     * @param action действие над товаром (например, "PLUS" или "MINUS")
     * @param model  модель для передачи обновлённой корзины
     * @return имя шаблона страницы корзины
     */
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
