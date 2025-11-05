package io.github.habatoo.servicies;

import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.dto.response.ItemDto;

/**
 * Интерфейс для работы с корзиной.
 * Предоставляет бизнес-логику для операций с товарами в корзине.
 */
public interface CartService {

    /**
     * Эндпоинт уменьшения/увеличения количества товара в корзине со страницы товаров в корзине
     * POST /items?id=[id]&search=[search]&sort=[sort]&pageNumber=[pageNumber]&pageSize=[pageSize]&action=[action]
     * возвращает Редирект.
     *
     * @param request запрос на изменение количества товара
     */
    void changeNumberOfItems(ChangeNumberOfItemsRequestDto request);

    /**
     * Эндпоинт уменьшения/увеличения количества товара в корзине со страницы товара в корзине
     * POST /items/{id}?action=[action]
     *
     * @param request запрос на изменение количества товара
     * @return объект товара ItemDto
     */
    ItemDto changeNumberOfItemsFromPage(ChangeNumberOfItemsRequestDto request);

    /**
     * Эндпоинт получения страницы со списком товаров в корзине
     * GET /cart/items
     *
     * @return объект корзины с товаром CartDto
     */
    CartDto getItemsInTheCart();

    /**
     * Эндпоинт уменьшения/увеличения количества товара в корзине со страницы корзины
     * POST /cart/items?id=[id]&action=[action]
     *
     * @param request запрос на изменение количества товара
     * @return объект корзины с товаром CartDto
     */
    CartDto changeNumberOfItemsFromCart(ChangeNumberOfItemsRequestDto request);
}
