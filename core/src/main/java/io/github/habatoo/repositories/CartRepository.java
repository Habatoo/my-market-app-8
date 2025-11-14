package io.github.habatoo.repositories;

import io.github.habatoo.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий работы с корзиной.
 */
public interface CartRepository extends JpaRepository<Cart, Long> {
}
