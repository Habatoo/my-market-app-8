package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.dto.response.OrderItemDto;
import io.github.habatoo.entity.Item;
import io.github.habatoo.entity.Order;
import io.github.habatoo.entity.OrderItem;
import io.github.habatoo.mappers.ItemMapper;
import io.github.habatoo.repositories.ItemRepository;
import io.github.habatoo.repositories.OrderItemRepository;
import io.github.habatoo.repositories.OrderRepository;
import io.github.habatoo.servicies.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final ItemMapper mapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public Flux<OrderDto> getOrders() {
        return orderRepository.findAll()
                .collectList()
                .flatMapMany(this::assembleOrders);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<OrderDto> getOrder(Long id, boolean newOrder) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalStateException("Заказ с id=" + id + " не найден")))
                .flatMap(order ->
                        assembleOrders(List.of(order)).single()
                );
    }

    private Flux<OrderDto> assembleOrders(List<Order> orders) {
        if (orders.isEmpty()) {
            return Flux.empty();
        }

        List<Long> orderIds = orders.stream()
                .map(Order::getId)
                .toList();

        return orderItemRepository.findAllByOrderIdIn(orderIds)
                .collectList()
                .flatMapMany(getMapper(orders));
    }

    private Function<List<OrderItem>, Publisher<? extends OrderDto>> getMapper(List<Order> orders) {
        return orderItems -> {
            List<Long> itemIds = orderItems.stream()
                    .map(OrderItem::getItemId)
                    .distinct()
                    .toList();

            return itemRepository.findAllById(itemIds)
                    .collectMap(Item::getId, item -> item)
                    .flatMapMany(itemMap -> Flux.fromIterable(buildOrderDtos(
                            orders, orderItems, itemMap)));
        };
    }

    private List<OrderDto> buildOrderDtos(List<Order> orders,
                                          List<OrderItem> orderItems,
                                          Map<Long, Item> itemMap) {

        Map<Long, List<OrderItem>> itemsByOrder = orderItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        return orders.stream()
                .map(order -> {
                    List<OrderItemDto> itemDtos = getItemDtos(itemMap, order, itemsByOrder);

                    return OrderDto.builder()
                            .id(order.getId())
                            .items(itemDtos)
                            .totalSum(order.getTotalSum())
                            .dateTime(order.getDateTime())
                            .build();
                })
                .toList();
    }

    private List<OrderItemDto> getItemDtos(
            Map<Long, Item> itemMap,
            Order order,
            Map<Long, List<OrderItem>> itemsByOrder) {
        return itemsByOrder
                .getOrDefault(order.getId(), List.of())
                .stream()
                .map(oi -> {
                    Item item = itemMap.get(oi.getItemId());
                    return OrderItemDto.builder()
                            .item(mapper.toDto(item))
                            .count(oi.getCount())
                            .price(oi.getPrice())
                            .total(oi.getPrice().multiply(BigDecimal.valueOf(oi.getCount())))
                            .build();
                })
                .toList();
    }
}
