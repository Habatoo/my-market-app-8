package io.github.habatoo.dto.response;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Позиция товара в заказе пользователя.
 *
 * @param item  Товар, добавленный в заказ
 * @param count Количество единиц товара в заказе
 * @param price Цена товара на момент оформления заказа
 * @param total Общая цена товара на момент оформления заказа
 */
@Builder
public record OrderItemDto(
        ItemDto item,
        Integer count,
        BigDecimal price,
        BigDecimal total
) {
}
