package io.github.habatoo.repositories;

import io.github.habatoo.entity.Cart;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/**
 * Репозиторий работы с корзиной.
 */
public interface CartRepository extends ReactiveCrudRepository<Cart, Long> {
}
