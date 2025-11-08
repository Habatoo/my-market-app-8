package io.github.habatoo.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Корзина покупателя в интернет-магазине.
 *
 * @param id идентификатор корзины
 * @param items покупки в корзине
 * @param total итоговая цена
 */
public record CartDto(
        Long id,
        List<CartItemDto> items,
        BigDecimal total
) {
    public CartDto {
        if (items == null) items = List.of();
    }

    public int getCountByItemId(Long itemId) {
        if (items == null) return 0;
        for (CartItemDto ci : items) {
            if (ci.item().id().equals(itemId)) {
                return ci.count();
            }
        }
        return 0;
    }
}
