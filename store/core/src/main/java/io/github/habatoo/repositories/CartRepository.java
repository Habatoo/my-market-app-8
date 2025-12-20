package io.github.habatoo.repositories;

import io.github.habatoo.entity.Cart;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * Репозиторий работы с корзиной.
 */
public interface CartRepository extends ReactiveCrudRepository<Cart, Long> {
    Mono<Cart> findByUserId(Long userId);
}
