package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.dto.response.OrderItemDto;
import io.github.habatoo.repositories.ItemRepository;
import io.github.habatoo.repositories.OrderItemRepository;
import io.github.habatoo.repositories.OrderRepository;
import io.github.habatoo.servicies.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Реализация для работы с заказами.
 * Предоставляет бизнес-логику для операций с отображением заказов и совершением покупки.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ItemRepository itemRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public Flux<OrderDto> getOrders() {
        return orderRepository.findAll()
                .flatMap(order ->
                        orderItemRepository.findAllByOrderId(order.getId())
                                .flatMap(orderItem ->
                                        itemRepository.findById(orderItem.getItemId())
                                                .map(item -> OrderItemDto.builder()
                                                        .item(new ItemDto(
                                                                item.getId(),
                                                                item.getTitle(),
                                                                item.getDescription(),
                                                                item.getImgPath(),
                                                                item.getPrice(),
                                                                0))
                                                        .count(orderItem.getCount())
                                                        .price(orderItem.getPrice())
                                                        .total(orderItem.getPrice().multiply(
                                                                BigDecimal.valueOf(orderItem.getCount())))
                                                        .build()
                                                )
                                )
                                .collectList()
                                .map(orderItemsDto -> OrderDto.builder()
                                        .id(order.getId())
                                        .items(orderItemsDto)
                                        .totalSum(order.getTotalSum())
                                        .dateTime(order.getDateTime())
                                        .build()
                                )
                );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<OrderDto> getOrder(Long id, boolean newOrder) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalStateException("Заказ с id=" + id + " не найден")))
                .flatMap(order ->
                        orderItemRepository.findAllByOrderId(order.getId())
                                .flatMap(orderItem ->
                                        itemRepository.findById(orderItem.getItemId())
                                                .map(item -> OrderItemDto.builder()
                                                        .item(new ItemDto(
                                                                item.getId(),
                                                                item.getTitle(),
                                                                item.getDescription(),
                                                                item.getImgPath(),
                                                                item.getPrice(),
                                                                0))
                                                        .count(orderItem.getCount())
                                                        .price(orderItem.getPrice())
                                                        .total(orderItem.getPrice().multiply(
                                                                BigDecimal.valueOf(orderItem.getCount())))
                                                        .build()
                                                )
                                )
                                .collectList()
                                .map(orderItemsDto -> OrderDto.builder()
                                        .id(order.getId())
                                        .items(orderItemsDto)
                                        .totalSum(order.getTotalSum())
                                        .dateTime(order.getDateTime())
                                        .build()
                                )
                );
    }
}
