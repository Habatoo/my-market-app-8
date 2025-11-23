package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.mappers.OrderMapper;
import io.github.habatoo.repositories.OrderRepository;
import io.github.habatoo.servicies.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Реализация для работы с заказами.
 * Предоставляет бизнес-логику для операций с отображением заказов и совершением покупки.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper mapper;

    /**
     * Получить список заказов с корректным расчетом сумм DTO
     */
    @Transactional(readOnly = true)
    @Override
    public Flux<OrderDto> getOrders() {
        log.debug("Запрашивается список всех заказов");

        return orderRepository.findAll()
                .doOnSubscribe(s -> log.debug("Начато получение заказов"))
                .doOnNext(order -> log.trace("Получен Order: {}", order.getId()))
                .map(mapper::toDto)
                .doOnNext(dto -> log.trace("Сформирован OrderDto: {}", dto.id()))
                .doOnComplete(() -> log.debug("Возврат списка DTO заказов завершён"));
    }

    /**
     * Получить конкретный заказ по id
     */
    @Transactional(readOnly = true)
    @Override
    public Mono<OrderDto> getOrder(Long id, boolean newOrder) {
        log.debug("Запрошен заказ по id={}, newOrder={}", id, newOrder);

        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalStateException("Заказ с id=%d не найден".formatted(id))))
                .map(order -> {
                    OrderDto dto = mapper.toDto(order);
                    log.info("Заказ найден и преобразован: orderId={}, totalSum={}", dto.id(), dto.totalSum());

                    return dto;
                });
    }
}
