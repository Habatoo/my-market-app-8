package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.entity.Order;
import io.github.habatoo.mappers.OrderMapper;
import io.github.habatoo.repositories.OrderRepository;
import io.github.habatoo.servicies.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Реализация для работы с заказами.
 * Предоставляет бизнес-логику для операций с отображением заказов и совершением покупки.
 */
@Slf4j
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
    public Flux<OrderDto> getOrders() {
        log.debug("Запрашивается список всех заказов");

        List<Order> orders = orderRepository.findAll();

        log.info("Найдено заказов: {}", orders.size());

        List<OrderDto> dtos = orders.stream()
                .map(mapper::toDto)
                .toList();

        log.debug("Возврат списка DTO заказов, всего: {}", dtos.size());

        return Flux.fromIterable(dtos);
    }

    /**
     * Получить конкретный заказ по id
     */
    @Transactional(readOnly = true)
    @Override
    public Mono<OrderDto> getOrder(Long id, boolean newOrder) {
        log.debug("Запрошен заказ по id={}, newOrder={}", id, newOrder);

        OrderDto orderDto = orderRepository.findById(id)
                .map(order -> {
                    OrderDto dto = mapper.toDto(order);

                    log.info("Заказ найден и преобразован: orderId={}, totalSum={}", dto.id(), dto.totalSum());

                    return dto;
                })
                .orElseThrow(() -> {
                    log.error("Заказ с id={} не найден", id);

                    return new IllegalStateException("Заказ с id=%d не найден".formatted(id));
                });

        return Mono.just(orderDto);
    }
}
