package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.dto.response.OrderItemDto;
import io.github.habatoo.entity.Item;
import io.github.habatoo.entity.Order;
import io.github.habatoo.entity.OrderItem;
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
import java.util.List;

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
                .flatMap(this::buildOrderDto);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<OrderDto> getOrder(Long id, boolean newOrder) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalStateException("Заказ с id=" + id + " не найден")))
                .flatMap(this::buildOrderDto);
    }

    /**
     * Собирает OrderDto: заказ + его позиции.
     */
    private Mono<OrderDto> buildOrderDto(Order order) {
        return loadOrderItems(order.getId())
                .map(orderItems -> OrderDto.builder()
                        .id(order.getId())
                        .items(orderItems)
                        .totalSum(order.getTotalSum())
                        .dateTime(order.getDateTime())
                        .build()
                );
    }

    /**
     * Загружает позиции заказа и преобразует их в OrderItemDto.
     */
    private Mono<List<OrderItemDto>> loadOrderItems(Long orderId) {
        return orderItemRepository.findAllByOrderId(orderId)
                .flatMap(this::buildOrderItemDto)
                .collectList();
    }

    /**
     * Собирает DTO позиции заказа, включая загрузку Item.
     */
    private Mono<OrderItemDto> buildOrderItemDto(OrderItem orderItem) {
        return itemRepository.findById(orderItem.getItemId())
                .map(item -> OrderItemDto.builder()
                        .item(mapToItemDto(item))
                        .count(orderItem.getCount())
                        .price(orderItem.getPrice())
                        .total(orderItem.getPrice().multiply(BigDecimal.valueOf(orderItem.getCount())))
                        .build());
    }

    /**
     * Преобразует Item → ItemDto.
     */
    private ItemDto mapToItemDto(Item item) {
        return new ItemDto(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getImgPath(),
                item.getPrice(),
                0
        );
    }
}
