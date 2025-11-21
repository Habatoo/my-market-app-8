package io.github.habatoo.controllers.root;

import io.github.habatoo.controllers.RootRedirectController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;

/**
 * Unit-тест для RootRedirectController.
 * Проверяет корректность перенаправления со старта приложения на витрину товаров.
 */
@DisplayName("Тесты для RootRedirectController")
class RootRedirectControllerTest {

    private static final String REDIRECT = "redirect:/items"; // тот же REDIRECT, что в контроллере

    private final RootRedirectController controller = new RootRedirectController();

    /**
     * Тест: контроллер должен возвращать редирект на /items.
     */
    @Test
    @DisplayName("GET \"/\" — возвращает редирект на витрину /items")
    void testRedirectToItems() {
        Mono<ResponseEntity<Void>> result = controller.redirectToItems();

        StepVerifier.create(result)
                .assertNext(response -> {
                    assert response.getStatusCode() == HttpStatus.FOUND;
                    assert (URI.create(REDIRECT).equals(response.getHeaders().getLocation()));
                })
                .verifyComplete();
    }
}
