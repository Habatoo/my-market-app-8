package io.github.habatoo.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Корзина покупателя в интернет-магазине.
 *
 * @param items покупки в корзине
 * @param total итоговая цена
 */
public record CartDto(
        List<CartItemDto> items,
        BigDecimal total
) {
}
