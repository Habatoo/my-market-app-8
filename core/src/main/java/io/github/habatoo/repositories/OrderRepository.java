package io.github.habatoo.repositories;

import io.github.habatoo.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий работы с заказами.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {
}
