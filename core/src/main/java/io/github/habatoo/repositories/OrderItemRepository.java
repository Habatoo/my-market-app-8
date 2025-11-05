package io.github.habatoo.repositories;

import io.github.habatoo.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий работы с товарами заказов.
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
