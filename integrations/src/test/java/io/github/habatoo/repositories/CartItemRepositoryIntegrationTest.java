package io.github.habatoo.repositories;

import io.github.habatoo.entity.Cart;
import io.github.habatoo.entity.CartItem;
import io.github.habatoo.entity.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Интеграционные тесты для CartItemRepository.
 * Проверяют сохранение, поиск, удаление позиций корзины, а также обработку невалидных связей.
 */
@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Интеграционные тесты CartItemRepository")
class CartItemRepositoryIntegrationTest {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    @BeforeEach
    void cleanUp() {
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        itemRepository.deleteAll();
    }

    @Test
    @DisplayName("Сохранение CartItem и поиск по id")
    void findSavedCartItemByIdTest() {
        Cart cart = createAndSaveCart();
        Item item = createAndSaveItem("CartItem1", BigDecimal.valueOf(15));
        CartItem cartItem = createAndSaveCartItem(cart, item, 2, BigDecimal.valueOf(15));

        Optional<CartItem> found = cartItemRepository.findById(cartItem.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getCart().getId()).isEqualTo(cart.getId());
        assertThat(found.get().getItem().getId()).isEqualTo(item.getId());
        assertThat(found.get().getCount()).isEqualTo(2);
        assertThat(found.get().getPrice()).isEqualTo(BigDecimal.valueOf(15));
    }

    @Test
    @DisplayName("Сохранение нескольких CartItem и поиск всех")
    void findAllCartItemsTest() {
        Cart cart = createAndSaveCart();
        Item item1 = createAndSaveItem("ItemA", BigDecimal.ONE);
        Item item2 = createAndSaveItem("ItemB", BigDecimal.TEN);

        createAndSaveCartItem(cart, item1, 1, BigDecimal.ONE);
        createAndSaveCartItem(cart, item2, 3, BigDecimal.TEN);

        List<CartItem> items = cartItemRepository.findAll();
        assertThat(items).hasSize(2);
    }

    @Test
    @DisplayName("Сохранение CartItem с валидной связкой Cart и Item")
    void createCartItemWithValidRelationsTest() {
        Cart cart = createAndSaveCart(BigDecimal.valueOf(10));
        Item item = createAndSaveItem("CartBindItem", BigDecimal.valueOf(10));
        CartItem cartItem = createAndSaveCartItem(cart, item, 2, BigDecimal.valueOf(10));

        Optional<CartItem> found = cartItemRepository.findById(cartItem.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getCart().getId()).isEqualTo(cart.getId());
        assertThat(found.get().getItem().getId()).isEqualTo(item.getId());
        assertThat(found.get().getCount()).isEqualTo(2);
        assertThat(found.get().getPrice()).isEqualTo(BigDecimal.valueOf(10));
    }

    @Test
    @DisplayName("Добавление нескольких CartItem и проверка связи с корзиной")
    void createMultipleItemsAndCartRelationTest() {
        Cart cart = createAndSaveCart(BigDecimal.valueOf(20));
        Item item1 = createAndSaveItem("A", BigDecimal.ONE);
        Item item2 = createAndSaveItem("B", BigDecimal.TEN);

        createAndSaveCartItem(cart, item1, 3, BigDecimal.ONE);
        createAndSaveCartItem(cart, item2, 5, BigDecimal.TEN);

        List<CartItem> cartItems = cartItemRepository.findAll();
        assertThat(cartItems).hasSize(2);
        List<Long> cartsIds = cartItems.stream()
                .map(ci -> ci.getCart().getId())
                .distinct().toList();
        assertThat(cartsIds).containsOnly(cart.getId());
    }

    @Test
    @DisplayName("Поиск CartItem по несуществующему id")
    void findCartItemByNonExistingIdTest() {
        Optional<CartItem> found = cartItemRepository.findById(-7777L);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Создание CartItem без Cart вызывает ошибку")
    void createCartItemWithoutCartTest() {
        Item item = createAndSaveItem("NoCart", BigDecimal.valueOf(9));
        CartItem cartItem = new CartItem();
        cartItem.setItem(item);
        cartItem.setCount(1);
        cartItem.setPrice(BigDecimal.valueOf(9));
        assertThatThrownBy(() -> cartItemRepository.saveAndFlush(cartItem))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Создание CartItem без Item вызывает ошибку")
    void createCartItemWithoutItemTest() {
        Cart cart = createAndSaveCart(BigDecimal.valueOf(5));
        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setCount(1);
        cartItem.setPrice(BigDecimal.valueOf(5));
        assertThatThrownBy(() -> cartItemRepository.saveAndFlush(cartItem))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Удаление CartItem по id")
    void deleteCartItemByIdTest() {
        Cart cart = createAndSaveCart(BigDecimal.ONE);
        Item item = createAndSaveItem("ToDelete", BigDecimal.ONE);
        CartItem cartItem = createAndSaveCartItem(cart, item, 1, BigDecimal.ONE);

        cartItemRepository.deleteById(cartItem.getId());
        Optional<CartItem> deleted = cartItemRepository.findById(cartItem.getId());
        assertThat(deleted).isEmpty();
    }

    /**
     * Создать и сохранить Cart с указанной суммой.
     *
     * @param total итоговая сумма корзины
     * @return Cart с присвоенным id
     */
    private Cart createAndSaveCart(BigDecimal total) {
        Cart cart = new Cart();
        cart.setTotal(total);
        cart.setItems(new ArrayList<>());
        return cartRepository.save(cart);
    }

    /**
     * Создать и сохранить Cart с total = 0.
     *
     * @return Новый пустой Cart
     */
    private Cart createAndSaveCart() {
        return createAndSaveCart(BigDecimal.ZERO);
    }

    /**
     * Создать и сохранить Item с указанными параметрами.
     *
     * @param title название
     * @param price цена товара
     * @return Item с присвоенным id
     */
    private Item createAndSaveItem(String title, BigDecimal price) {
        Item item = new Item();
        item.setTitle(title);
        item.setDescription("desc_" + title);
        item.setImgPath("img/" + title);
        item.setPrice(price);
        return itemRepository.save(item);
    }

    /**
     * Создать и сохранить CartItem для указанной корзины и товара.
     *
     * @param cart  корзина
     * @param item  товар
     * @param count количество
     * @param price цена позиции
     * @return CartItem с присвоенным id
     */
    private CartItem createAndSaveCartItem(Cart cart, Item item, int count, BigDecimal price) {
        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setItem(item);
        cartItem.setCount(count);
        cartItem.setPrice(price);
        return cartItemRepository.save(cartItem);
    }
}
