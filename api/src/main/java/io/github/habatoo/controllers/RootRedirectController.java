package io.github.habatoo.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Корневой контроллер перенаправления.
 * Все запросы к корню "/" сразу перенаправляются на страницу витрины товаров "/items".
 */
@Controller
@RequestMapping("/")
public class RootRedirectController {

    private static final String REDIRECT = "redirect:/items";

    /**
     * Перенаправляет пользователя с корня сайта на страницу товаров.
     *
     * @return строка редиректа для Spring
     */
    @GetMapping
    public String redirectToItems() {
        return REDIRECT;
    }
}
