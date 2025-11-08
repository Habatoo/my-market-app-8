package io.github.habatoo.repositories;

import io.github.habatoo.entity.Item;
import io.github.habatoo.entity.Order;
import io.github.habatoo.entity.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Интеграционные тесты для связи между OrderItem–Order–Item через OrderItemRepository.
 * Проверяет создание позиций заказа, корректные связи, выборку и обработку несуществующих связей.
 */
@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Тесты связей OrderItem - Order - Item")
class OrderItemRepositoryIntegrationTest {

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
    @DisplayName("Создание OrderItem с валидными Order и Item")
    void createOrderItemWithValidRelationsTest() {
        Item item = createAndSaveItem("OrderBindItem", BigDecimal.valueOf(19));
        Order order = createAndSaveOrder(BigDecimal.valueOf(19), LocalDateTime.now());
        OrderItem orderItem = createAndSaveOrderItem(order, item, 3, BigDecimal.valueOf(19));

        Optional<OrderItem> found = orderItemRepository.findById(orderItem.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getOrder().getId()).isEqualTo(order.getId());
        assertThat(found.get().getItem().getId()).isEqualTo(item.getId());
        assertThat(found.get().getCount()).isEqualTo(3);
        assertThat(found.get().getPrice()).isEqualTo(BigDecimal.valueOf(19));
    }

    @Test
    @DisplayName("Добавление нескольких OrderItem и проверка связи с заказом")
    void createMultipleItemsAndOrderRelationTest() {
        Order order = createAndSaveOrder(BigDecimal.valueOf(35), LocalDateTime.now());
        Item item1 = createAndSaveItem("A", BigDecimal.valueOf(10));
        Item item2 = createAndSaveItem("B", BigDecimal.valueOf(25));

        createAndSaveOrderItem(order, item1, 1, BigDecimal.valueOf(10));
        createAndSaveOrderItem(order, item2, 2, BigDecimal.valueOf(25));

        List<OrderItem> orderItems = orderItemRepository.findAll();
        assertThat(orderItems).hasSize(2);
        List<Long> itemsOrderIds = orderItems.stream()
                .map(oi -> oi.getOrder().getId())
                .distinct().toList();
        assertThat(itemsOrderIds).containsOnly(order.getId());
    }

    @Test
    @DisplayName("Поиск OrderItem по несуществующему id")
    void findOrderItemByNonExistingIdTest() {
        Optional<OrderItem> found = orderItemRepository.findById(-12345L);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Создание OrderItem без привязки к Order кидает ошибку")
    void createOrderItemWithoutOrderTest() {
        Item item = createAndSaveItem("NoOrder", BigDecimal.valueOf(12));
        OrderItem orderItem = new OrderItem();
        orderItem.setItem(item);
        orderItem.setCount(1);
        orderItem.setPrice(BigDecimal.valueOf(12));

        assertThatThrownBy(() -> orderItemRepository.saveAndFlush(orderItem))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Создание OrderItem без привязки к Item кидает ошибку")
    void createOrderItemWithoutItemTest() {
        Order order = createAndSaveOrder(BigDecimal.valueOf(1), LocalDateTime.now());
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setCount(1);
        orderItem.setPrice(BigDecimal.ONE);
        assertThatThrownBy(() -> orderItemRepository.saveAndFlush(orderItem))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Удаление OrderItem по id")
    void deleteOrderItemByIdTest() {
        Item item = createAndSaveItem("ToDelete", BigDecimal.valueOf(8));
        Order order = createAndSaveOrder(BigDecimal.valueOf(8), LocalDateTime.now());
        OrderItem orderItem = createAndSaveOrderItem(order, item, 1, BigDecimal.valueOf(8));
        orderItemRepository.deleteById(orderItem.getId());
        Optional<OrderItem> deleted = orderItemRepository.findById(orderItem.getId());
        assertThat(deleted).isEmpty();
    }

    /**
     * Вспомогательный метод создания и сохранения Item.
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
     * Вспомогательный метод создания и сохранения Order.
     */
    private Order createAndSaveOrder(BigDecimal totalSum, LocalDateTime dateTime) {
        Order order = new Order();
        order.setTotalSum(totalSum);
        order.setDateTime(dateTime);
        order.setItems(new ArrayList<>());
        return orderRepository.save(order);
    }

    /**
     * Создать и сохранить OrderItem, привязав к нему заказ и товар.
     */
    private OrderItem createAndSaveOrderItem(Order order, Item item, int count, BigDecimal price) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setItem(item);
        orderItem.setCount(count);
        orderItem.setPrice(price);
        return orderItemRepository.save(orderItem);
    }
}

