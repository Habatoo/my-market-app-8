package io.github.habatoo.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Корневой контроллер перенаправления.
 * Все запросы к корню "/" сразу перенаправляются на страницу витрины товаров "/items".
 */
@Slf4j
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
    public Mono<String> redirectToItems() {
        log.info("GET / — редирект на {}", REDIRECT);

        return Mono.just(REDIRECT);
    }
}
