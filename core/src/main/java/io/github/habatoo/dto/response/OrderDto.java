package io.github.habatoo.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Заказ пользователя в интернет-магазине.
 *
 * @param items    Список позиций в заказе
 * @param totalSum Итоговая сумма заказа
 * @param dateTime Дата и время оформления заказа
 */
public record OrderDto(
        List<OrderItemDto> items,
        BigDecimal totalSum,
        LocalDateTime dateTime
) {
}
