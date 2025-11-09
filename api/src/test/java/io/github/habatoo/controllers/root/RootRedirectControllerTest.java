package io.github.habatoo.controllers.root;

import io.github.habatoo.controllers.RootRedirectController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RootRedirectControllerTest {

    @Test
    void testRedirectToItems() {
        RootRedirectController controller = new RootRedirectController();

        String result = controller.redirectToItems();

        assertEquals("redirect:/items", result);
    }
}
