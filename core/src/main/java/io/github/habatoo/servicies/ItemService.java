package io.github.habatoo.servicies;

import io.github.habatoo.dto.request.GetItemsRequestDto;
import io.github.habatoo.dto.response.ItemDto;

import java.util.List;

/**
 * Интерфейс для работы с товарами.
 * Предоставляет бизнес-логику для операций с отображением товаров на витрине.
 */
public interface ItemService {

    /**
     * Эндпоинт получения товаров на странице.
     * GET /?search=[search]&sort=[sort]&pageNumber=[pageNumber]&pageSize=[pageSize]
     * GET /items?search=[search]&sort=[sort]&pageNumber=[pageNumber]&pageSize=[pageSize]
     *
     * @param request запрос товаров
     * @return список списков товаров ItemDto
     */
    List<List<ItemDto>> getItems(GetItemsRequestDto request);

    /**
     * Эндпоинт получения страницы с товаром
     * GET /items/{id}
     *
     * @param id идентификатор товара
     * @return объект товара ItemDto
     */
    ItemDto getItem(Long id);
}
