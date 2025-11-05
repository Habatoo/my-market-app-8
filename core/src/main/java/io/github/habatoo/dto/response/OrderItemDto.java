package io.github.habatoo.dto.response;

import java.math.BigDecimal;

/**
 * Позиция товара в заказе пользователя.
 *
 * @param item  Товар, добавленный в заказ
 * @param order Заказ, к которому относится данная позиция
 * @param count Количество единиц товара в заказе
 * @param price Цена товара на момент оформления заказа
 */
public record OrderItemDto(
        ItemDto item,
        OrderDto order,
        Integer count,
        BigDecimal price
) {
}
