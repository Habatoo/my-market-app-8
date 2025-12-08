package io.github.habatoo.utils;

import io.github.habatoo.entity.*;
import io.github.habatoo.repositories.*;
import io.github.habatoo.store.payment.api.PaymentsApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Testcontainers
public abstract class BaseTest {

    @Autowired
    protected CartRepository cartRepository;

    @Autowired
    protected CartItemRepository cartItemRepository;

    @Autowired
    protected OrderItemRepository orderItemRepository;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected ItemRepository itemRepository;

    @MockitoBean
    protected PaymentsApi paymentsApi;

    @BeforeEach
    void cleanUp() {
        cleanDataBase();
    }

    @AfterEach
    void tearDown() {
        cleanDataBase();
    }

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    /**
     * Создать и сохранить Cart с указанной суммой.
     */
    protected Mono<Cart> createAndSaveCart(BigDecimal total) {
        Cart cart = new Cart();
        cart.setTotal(total);
        return cartRepository.save(cart);
    }

    protected Mono<Cart> createAndSaveCart() {
        return createAndSaveCart(BigDecimal.ZERO);
    }

    /**
     * Создать и сохранить Item с указанными параметрами.
     */
    protected Mono<Item> createAndSaveItem(String title, BigDecimal price) {
        Item item = new Item();
        item.setTitle(title);
        item.setDescription("desc_" + title);
        item.setImgPath("img/" + title);
        item.setPrice(price);
        return itemRepository.save(item);
    }

    /**
     * Создать и сохранить CartItem для указанной корзины и товара.
     */
    protected Mono<CartItem> createAndSaveCartItem(Cart cart, Item item, int count, BigDecimal price) {
        CartItem cartItem = new CartItem();
        cartItem.setCartId(cart.getId());
        cartItem.setItemId(item.getId());
        cartItem.setCount(count);
        cartItem.setPrice(price);
        return cartItemRepository.save(cartItem);
    }

    /**
     * Создание и сохранение Order с указанной суммой и датой.
     */
    protected Mono<Order> createAndSaveOrder(BigDecimal totalSum, LocalDateTime dateTime) {
        Order order = new Order();
        order.setTotalSum(totalSum);
        order.setDateTime(dateTime);
        return orderRepository.save(order);
    }

    /**
     * Создание и сохранение OrderItem, связываем с orderId и itemId.
     */
    protected Mono<OrderItem> createAndSaveOrderItem(Order order, Item item, int count, BigDecimal price) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(order.getId());
        orderItem.setItemId(item.getId());
        orderItem.setCount(count);
        orderItem.setPrice(price);
        return orderItemRepository.save(orderItem);
    }

    private void cleanDataBase() {
        orderItemRepository.deleteAll()
                .then(orderRepository.deleteAll())
                .then(cartItemRepository.deleteAll())
                .then(cartRepository.deleteAll())
                .then(itemRepository.deleteAll())
                .block();
    }
}
