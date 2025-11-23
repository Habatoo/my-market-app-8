package io.github.habatoo.repositories;

import io.github.habatoo.entity.OrderItem;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/**
 * Репозиторий работы с товарами заказов.
 */
public interface OrderItemRepository extends ReactiveCrudRepository<OrderItem, Long> {
}
