package io.github.habatoo.repositories;

import io.github.habatoo.entity.Cart;
import io.github.habatoo.entity.CartItem;
import io.github.habatoo.entity.Item;
import io.github.habatoo.utils.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты для CartRepository.
 * Проверяет сохранение, поиск и удаление корзины, а также корректность хранения суммы и наличия позиций.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Интеграционные тесты CartRepository")
class CartRepositoryIntegrationTest extends BaseTest {

    @Test
    @DisplayName("Сохранение и выборка корзины по id")
    void findSavedCartByIdTest() {
        Cart cart = createAndSaveCart(BigDecimal.valueOf(100));
        Optional<Cart> found = cartRepository.findById(cart.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTotal()).isEqualTo(BigDecimal.valueOf(100));
    }

    @Test
    @DisplayName("Сохранение корзины с позициями и проверка связей")
    void saveCartWithItemsTest() {
        Cart cart = createAndSaveCart(BigDecimal.valueOf(50));
        Item item = createAndSaveItem("CartRepoItem", BigDecimal.valueOf(25));
        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setItem(item);
        cartItem.setCount(2);
        cartItem.setPrice(BigDecimal.valueOf(25));
        cartItemRepository.save(cartItem);

        cart.getItems().add(cartItem);
        cartRepository.save(cart);

        Optional<Cart> found = cartRepository.findById(cart.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getItems()).hasSize(1);
        CartItem foundItem = found.get().getItems().get(0);
        assertThat(foundItem.getItem().getTitle()).isEqualTo("CartRepoItem");
        assertThat(foundItem.getCount()).isEqualTo(2);
    }


    @Test
    @DisplayName("Удаление корзины и проверка, что она не найдена")
    void deleteCartByIdTest() {
        Cart cart = createAndSaveCart(BigDecimal.ZERO);
        Long id = cart.getId();

        cartRepository.deleteById(id);

        Optional<Cart> deleted = cartRepository.findById(id);
        assertThat(deleted).isEmpty();
    }

    @Test
    @DisplayName("Поиск всех корзин")
    void findAllCartsTest() {
        createAndSaveCart(BigDecimal.valueOf(10));
        createAndSaveCart(BigDecimal.valueOf(50));
        List<Cart> carts = cartRepository.findAll();
        assertThat(carts).hasSize(2);
    }
}
