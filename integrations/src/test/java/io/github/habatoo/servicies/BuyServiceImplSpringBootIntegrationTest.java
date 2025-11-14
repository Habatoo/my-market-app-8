package io.github.habatoo.servicies;

import io.github.habatoo.entity.*;
import io.github.habatoo.utils.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционный тест для BuyServiceImpl с использованием @SpringBootTest.
 * Проверяет успешное оформление покупки и работу со всеми репозиториями.
 */
@Transactional
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Интеграционный тест BuyServiceImpl — оформление покупки")
class BuyServiceImplSpringBootIntegrationTest extends BaseTest {

    @Autowired
    private BuyService buyService;

    /**
     * Тест — успешная покупка переносит товары из корзины в заказ, корзина очищается.
     */
    @ParameterizedTest
    @DisplayName("Оформить покупку — из корзины формируется заказ для разных товаров и количества, корзина очищается")
    @CsvSource({
            "ТоварA, 100, 3",
            "ТоварB, 45.50, 2",
            "ТоварC, 5, 10"
    })
    void buyFromCartSuccessParameterizedTest(String title, BigDecimal price, int count) {
        Item item = createAndSaveItem(title, price);

        Cart cart = new Cart();
        Cart savedCart = cartRepository.save(cart);

        CartItem cartItem = new CartItem();
        cartItem.setCart(savedCart);
        cartItem.setItem(item);
        cartItem.setCount(count);
        cartItem.setPrice(item.getPrice());
        savedCart.setItems(new ArrayList<>(List.of(cartItem)));
        savedCart.setTotal(item.getPrice().multiply(BigDecimal.valueOf(count)));
        cartItemRepository.save(cartItem);
        cartRepository.save(savedCart);

        buyService.buy(cart.getId());

        Cart cartAfter = cartRepository.findById(cart.getId()).orElseThrow();
        assertEquals(BigDecimal.ZERO, cartAfter.getTotal());
        assertTrue(cartAfter.getItems().isEmpty());

        List<Order> orders = orderRepository.findAll();
        assertFalse(orders.isEmpty());
        Order order = orders.get(orders.size() - 1);
        assertEquals(item.getPrice().multiply(BigDecimal.valueOf(count)), order.getTotalSum());
        assertEquals(1, order.getItems().size());

        OrderItem oi = order.getItems().get(0);
        assertEquals(count, oi.getCount());
        assertEquals(item.getPrice(), oi.getPrice());
    }

    /**
     * Тест — если корзина не найдена, выбрасывается IllegalStateException.
     */
    @Test
    @DisplayName("Ошибка — при отсутствии корзины выбрасывается IllegalStateException")
    void buyCartNotFoundTest() {
        Long wrongId = 999999L;
        assertThrows(IllegalStateException.class, () -> buyService.buy(wrongId));
    }
}
