package io.github.habatoo.controllers.root;

import io.github.habatoo.controllers.RootRedirectController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit-тест для RootRedirectController.
 * Проверяет корректность перенаправления со старта приложения на витрину товаров.
 */
@DisplayName("Тесты для RootRedirectController")
class RootRedirectControllerTest {

    /**
     * Тест: контроллер должен возвращать редирект на /items.
     */
    @Test
    @DisplayName("GET \"/\" — возвращает редирект на витрину /items")
    void testRedirectToItems() {
        RootRedirectController controller = new RootRedirectController();

        String result = controller.redirectToItems();

        assertEquals("redirect:/items", result);
    }
}
