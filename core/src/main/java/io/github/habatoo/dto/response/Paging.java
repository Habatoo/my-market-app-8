package io.github.habatoo.dto.response;

import lombok.Builder;

/**
 * DTO, описывающее параметры пагинации для списка данных (например, товаров).
 * Используется для передачи информации о размере страницы, текущем номере,
 * наличии предыдущей и следующей страницы, а также общем количестве результатов.
 *
 * @param total Общее количество элементов (товаров, заказов и т.п.) для пагинации
 * @param pageSize Размер страницы (число элементов на одну страницу)
 * @param pageNumber Номер текущей страницы (начиная с 1)
 * @param hasPrevious  Признак наличия предыдущей страницы (true, если можно перейти назад)
 * @param hasNext Признак наличия следующей страницы (true, если можно перейти вперёд)
 */


@Builder
public record Paging(
        int total,
        int pageSize,
        int pageNumber,
        boolean hasPrevious,
        boolean hasNext) {
}
