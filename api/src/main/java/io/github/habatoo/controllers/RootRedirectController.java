package io.github.habatoo.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class RootRedirectController {

    private static final String REDIRECT = "redirect:/items";

    @GetMapping
    public String redirectToItems() {
        return REDIRECT;
    }
}
