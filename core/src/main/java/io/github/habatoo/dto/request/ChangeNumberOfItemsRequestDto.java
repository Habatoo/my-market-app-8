package io.github.habatoo.dto.request;

import io.github.habatoo.dto.Action;
import io.github.habatoo.dto.Sort;
import lombok.Builder;
import lombok.Data;

/**
 * Универсальный DTO для запросов изменения количества товара (витрина, карточка товара, корзина).
 * Передаются только необходимые поля — остальные могут быть null.
 */
@Data
@Builder
public class ChangeNumberOfItemsRequestDto {

    /**
     * Идентификатор товара.
     */
    private Long id;

    /**
     * Действие: плюс/минус/удалить.
     */
    private Action action;

    /**
     * Параметры поиска и сортировки (используются только на витрине).
     */
    private String search;

    /**
     * Способ сортировки товаров
     */
    private Sort sort;

    /**
     * Номер текущей страницы с товарами (по умолчанию первая страница — 1)
     */
    private Integer pageNumber;

    /**
     * Число товаров на странице с товарами (по умолчанию первая страница — 5)
     */
    private Integer pageSize;
}

