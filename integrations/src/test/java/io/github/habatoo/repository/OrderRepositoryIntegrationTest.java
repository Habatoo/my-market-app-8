package io.github.habatoo.repository;

import io.github.habatoo.entity.Item;
import io.github.habatoo.entity.Order;
import io.github.habatoo.entity.OrderItem;
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

/**
 * Интеграционные тесты для OrderRepository.
 * Проверяет операции сохранения, поиска, удаления и работу связей с позициями заказа.
 */
@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Интеграционные тесты OrderRepository")
class OrderRepositoryIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    @DisplayName("Сохранение и поиск заказа по id")
    void findSavedOrderByIdTest() {
        LocalDateTime now = LocalDateTime.now();
        Order order = createAndSaveOrder(BigDecimal.valueOf(111), now);

        Optional<Order> found = orderRepository.findById(order.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTotalSum()).isEqualTo(BigDecimal.valueOf(111));
        assertThat(found.get().getDateTime()).isEqualTo(now);
    }

    @Test
    @DisplayName("Сохранение заказа с позициями и проверка связей")
    void saveOrderWithItemsTest() {
        Order order = createAndSaveOrder(BigDecimal.valueOf(222), LocalDateTime.now());
        Item item = createAndSaveItem("OrderItemA", BigDecimal.valueOf(111));
        createAndSaveOrderItem(order, item, 3, BigDecimal.valueOf(111));

        Optional<Order> found = orderRepository.findById(order.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getItems()).hasSize(1);
        OrderItem foundItem = found.get().getItems().get(0);
        assertThat(foundItem.getItem().getTitle()).isEqualTo("OrderItemA");
        assertThat(foundItem.getCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Удаление заказа по id")
    void deleteOrderByIdTest() {
        Order order = createAndSaveOrder(BigDecimal.valueOf(0), LocalDateTime.now());
        Long id = order.getId();

        orderRepository.deleteById(id);

        Optional<Order> deleted = orderRepository.findById(id);
        assertThat(deleted).isEmpty();
    }

    @Test
    @DisplayName("Поиск всех заказов")
    void findAllOrdersTest() {
        createAndSaveOrder(BigDecimal.valueOf(100), LocalDateTime.now());
        createAndSaveOrder(BigDecimal.valueOf(200), LocalDateTime.now());
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(2);
    }

    /**
     * Создаёт и сохраняет заказ с заданной суммой и временем.
     */
    private Order createAndSaveOrder(BigDecimal totalSum, LocalDateTime dateTime) {
        Order order = new Order();
        order.setTotalSum(totalSum);
        order.setDateTime(dateTime);
        order.setItems(new ArrayList<>());
        return orderRepository.save(order);
    }

    /**
     * Создаёт и сохраняет тестовый Item.
     */
    private Item createAndSaveItem(String title, BigDecimal price) {
        Item item = new Item();
        item.setTitle(title);
        item.setDescription("Order " + title);
        item.setImgPath("img/" + title);
        item.setPrice(price);
        return itemRepository.save(item);
    }

    /**
     * Добавляет позицию заказа к заказу.
     */
    private OrderItem createAndSaveOrderItem(Order order, Item item, int count, BigDecimal price) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setItem(item);
        orderItem.setCount(count);
        orderItem.setPrice(price);
        orderItemRepository.save(orderItem);

        order.getItems().add(orderItem);
        orderRepository.save(order);

        return orderItem;
    }
}
