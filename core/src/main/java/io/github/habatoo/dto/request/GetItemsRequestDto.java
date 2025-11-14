package io.github.habatoo.dto.request;

import io.github.habatoo.dto.enums.Sort;
import lombok.Builder;
import lombok.Data;

/**
 * DTO для запроса получения товаров на витрине.
 * Описывает параметры поиска и пагинации.
 */
@Data
@Builder
public class GetItemsRequestDto {

    /**
     * Строка поиска по названию/описанию товара.
     */
    private String search;

    /**
     * Способ сортировки товаров.
     */
    private Sort sort;

    /**
     * Номер текущей страницы с товарами (по умолчанию 1).
     */
    private Integer pageNumber;

    /**
     * Число товаров на странице (по умолчанию 5).
     */
    private Integer pageSize;
}
