package io.github.habatoo.repositories;

import io.github.habatoo.entity.CartItem;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Репозиторий работы с товарами в корзине.
 */
public interface CartItemRepository extends ReactiveCrudRepository<CartItem, Long> {
    Flux<CartItem> findAllByCartId(Long cartId);

    @Query("SELECT count FROM cart_items WHERE cart_id = :cartId AND item_id = :itemId")
    Mono<Integer> findCountByCartIdAndItemId(Long cartId, Long itemId);

    Mono<Void> deleteAllByCartId(Long cartId);
}
