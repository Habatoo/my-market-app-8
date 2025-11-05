package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.servicies.OrderService;

import java.util.List;

/**
 * Реализация для работы с заказами.
 * Предоставляет бизнес-логику для операций с отображением заказов и совершением покупки.
 */
public class OrderServiceImpl implements OrderService {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<OrderDto> getOrders() {
        return List.of();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OrderDto getOrder(Long id, boolean newOrder) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buy(Long id) {
    }
}
