package io.github.habatoo.dto.response;

import java.math.BigDecimal;

/**
 * Позиция товара в корзине покупателя.
 *
 * @param item  Товар в корзине
 * @param count Количество товара в позиции корзины
 * @param price Цена товара на момент добавления в корзину
 */
public record CartItemDto(
        ItemDto item,
        Integer count,
        BigDecimal price
) {
}
