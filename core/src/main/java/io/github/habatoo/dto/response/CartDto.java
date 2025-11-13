package io.github.habatoo.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * Корзина покупателя в интернет-магазине.
 *
 * @param id    идентификатор корзины
 * @param items покупки в корзине
 * @param total итоговая цена
 */
@Builder
public record CartDto(
        Long id,
        List<CartItemDto> items,
        BigDecimal total
) {
    public CartDto {
        if (items == null) items = List.of();
    }
}
