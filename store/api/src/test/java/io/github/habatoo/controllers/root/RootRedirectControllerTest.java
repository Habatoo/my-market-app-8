package io.github.habatoo.controllers.root;

import io.github.habatoo.controllers.RootRedirectController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit-тест для RootRedirectController.
 * Проверяет корректность перенаправления со старта приложения на витрину товаров.
 */
@DisplayName("Тесты для RootRedirectController")
class RootRedirectControllerTest {

    private static final String REDIRECT = "redirect:/items";

    private final RootRedirectController controller = new RootRedirectController();

    /**
     * Тест: контроллер должен возвращать редирект на /items.
     */
    @Test
    @DisplayName("GET \"/\" — возвращает редирект на витрину /items")
    void testRedirectToItems() {
        Mono<String> result = controller.redirectToItems();

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(REDIRECT, response);
                })
                .verifyComplete();
    }
}
