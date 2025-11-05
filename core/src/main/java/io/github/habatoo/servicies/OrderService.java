package io.github.habatoo.servicies;

import io.github.habatoo.dto.response.OrderDto;

import java.util.List;

/**
 * Интерфейс для работы с заказами.
 * Предоставляет бизнес-логику для операций с отображением заказов и совершением покупки.
 */
public interface OrderService {

    /**
     * Эндпоинт получения страницы со списком заказов
     * GET /orders
     *
     * @return объект со списком заказов OrderDto
     */
    List<OrderDto> getOrders();

    /**
     * Эндпоинт получения страницы заказа
     * GET /orders/{id}?newOrder=[newOrder]
     *
     * @param id       идентификатор заказа
     * @param newOrder [newOrder] — true, если совершён переход с кнопки покупки товаров (по умолчанию false)
     * @return объект заказа OrderDto
     */
    OrderDto getOrder(Long id, boolean newOrder);

    /**
     * Эндпоинт совершения заказа
     * POST /buy
     * Редирект: redirect:/orders/{id}?newOrder=true, где id — идентификатор созданного заказа.
     *
     * @param id идентификатор заказа
     */
    void buy(Long id);
}
