package io.github.habatoo.repositories;

import io.github.habatoo.entity.Item;
import io.github.habatoo.entity.Order;
import io.github.habatoo.entity.OrderItem;
import io.github.habatoo.utils.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты для OrderRepository.
 * Проверяет операции сохранения, поиска, удаления и работу связей с позициями заказа.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Интеграционные тесты CartItemRepository")
class OrderRepositoryIntegrationTest extends BaseTest {

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
}
