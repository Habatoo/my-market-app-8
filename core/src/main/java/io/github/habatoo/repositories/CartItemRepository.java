package io.github.habatoo.repositories;

import io.github.habatoo.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий работы с товарами в корзине.
 */
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}
