package io.github.habatoo.repositories;

import io.github.habatoo.entity.Order;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/**
 * Репозиторий работы с заказами.
 */
public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {
}
