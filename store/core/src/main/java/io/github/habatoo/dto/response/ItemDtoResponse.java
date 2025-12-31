package io.github.habatoo.dto.response;

import lombok.Builder;

/**
 * DTO ответа по товару
 *
 * @param item      объект товара
 * @param cartCount число товаров в корзине
 * @param isAuth    флаг авторизации
 */
@Builder
public record ItemDtoResponse(
        ItemDto item,
        Integer cartCount,
        boolean isAuth
) {
}
