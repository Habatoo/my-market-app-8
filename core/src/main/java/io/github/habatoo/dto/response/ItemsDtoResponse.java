package io.github.habatoo.dto.response;

import lombok.Builder;

import java.util.List;

/**
 * DTO ответа по списку товаров
 *
 * @param itemsRows списокт товаров для отображения
 * @param cart      объект корзины
 * @param paging    постраничное разбиение
 */
@Builder
public record ItemsDtoResponse(
        List<List<ItemDto>> itemsRows,
        CartDto cart,
        Paging paging
) {
}
