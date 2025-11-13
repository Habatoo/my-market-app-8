package io.github.habatoo.repositories;

import io.github.habatoo.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Репозиторий работы с товарами в корзине.
 */
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    @Query("SELECT ci.count FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.item.id = :itemId")
    Integer findCountByCartIdAndItemId(@Param("cartId") Long cartId, @Param("itemId") Long itemId);
}
