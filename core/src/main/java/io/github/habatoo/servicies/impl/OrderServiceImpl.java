package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.entity.Order;
import io.github.habatoo.mappers.OrderMapper;
import io.github.habatoo.repositories.OrderRepository;
import io.github.habatoo.servicies.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Реализация для работы с заказами.
 * Предоставляет бизнес-логику для операций с отображением заказов и совершением покупки.
 */
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper mapper;

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderMapper mapper) {
        this.orderRepository = orderRepository;
        this.mapper = mapper;
    }

    /**
     * Получить список заказов с корректным расчетом сумм DTO
     */
    @Transactional(readOnly = true)
    @Override
    public List<OrderDto> getOrders() {
        List<Order> orders = orderRepository.findAll();

        return orders.stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Получить конкретный заказ по id
     */
    @Transactional(readOnly = true)
    @Override
    public OrderDto getOrder(Long id, boolean newOrder) {
        return orderRepository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new IllegalStateException("Заказ с id=%d не найден".formatted(id)));
    }
}
