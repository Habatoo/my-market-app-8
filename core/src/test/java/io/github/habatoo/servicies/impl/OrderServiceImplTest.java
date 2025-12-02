package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.dto.response.OrderItemDto;
import io.github.habatoo.entity.Item;
import io.github.habatoo.entity.Order;
import io.github.habatoo.entity.OrderItem;
import io.github.habatoo.mappers.ItemMapper;
import io.github.habatoo.repositories.ItemRepository;
import io.github.habatoo.repositories.OrderItemRepository;
import io.github.habatoo.repositories.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Параметризованные unit-тесты для OrderServiceImpl.
 * Проверяют все бизнес-кейсы: получение списка заказов, пустой список, получение по id,
 * отсутствие заказа по id, корректное преобразование через маппер.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тест загрузки OrderServiceImpl (reactive)")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private ItemMapper mapper;
    @InjectMocks
    private OrderServiceImpl service;

    /**
     * Тест получения всех заказов с их позициями.
     */
    @Test
    @DisplayName("getOrders — корректный возврат заказа с позициями")
    void getOrdersTest() {
        Order order = createOrder(1L, BigDecimal.valueOf(100));
        OrderItem orderItem = createOrderItem(1L, 10L, 2, BigDecimal.valueOf(50));
        Item item = createItem(10L, "Item 10", BigDecimal.valueOf(50));
        ItemDto itemDto = createItemDto(10L, "Item 10", BigDecimal.valueOf(50));

        when(orderRepository.findAll()).thenReturn(Flux.just(order));
        when(orderItemRepository.findAllByOrderIdIn(List.of(1L))).thenReturn(Flux.just(orderItem));
        when(itemRepository.findAllById(List.of(10L))).thenReturn(Flux.just(item));
        when(mapper.toDto(item)).thenReturn(itemDto);

        StepVerifier.create(service.getOrders())
                .assertNext(orderDto -> verifyOrderDto(orderDto, order, orderItem, itemDto))
                .verifyComplete();

        verify(orderRepository).findAll();
        verify(orderItemRepository).findAllByOrderIdIn(List.of(1L));
        verify(itemRepository).findAllById(List.of(10L));
        verify(mapper).toDto(item);
    }

    /**
     * Тест получения заказа — не найден.
     */
    @Test
    @DisplayName("getOrder — заказ найден")
    void getOrderFoundTest() {
        Order order = createOrder(2L, BigDecimal.valueOf(200));
        OrderItem orderItem = createOrderItem(2L, 20L, 1, BigDecimal.valueOf(200));
        Item item = createItem(20L, "Item 20", BigDecimal.valueOf(200));
        ItemDto itemDto = createItemDto(20L, "Item 20", BigDecimal.valueOf(200));

        when(orderRepository.findById(2L)).thenReturn(Mono.just(order));
        when(orderItemRepository.findAllByOrderIdIn(List.of(2L))).thenReturn(Flux.just(orderItem));
        when(itemRepository.findAllById(List.of(20L))).thenReturn(Flux.just(item));
        when(mapper.toDto(item)).thenReturn(itemDto);

        StepVerifier.create(service.getOrder(2L, true))
                .assertNext(orderDto -> verifyOrderDto(orderDto, order, orderItem, itemDto))
                .verifyComplete();

        verify(orderRepository).findById(2L);
        verify(orderItemRepository).findAllByOrderIdIn(List.of(2L));
        verify(itemRepository).findAllById(List.of(20L));
        verify(mapper).toDto(item);
    }

    /**
     * Тест получения всех заказов — пустой список.
     */
    @Test
    @DisplayName("getOrders — пустой список")
    void getOrdersEmptyTest() {
        when(orderRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(service.getOrders())
                .expectNextCount(0)
                .verifyComplete();

        verify(orderRepository).findAll();
    }

    private Order createOrder(Long id, BigDecimal sum) {
        Order order = new Order();
        order.setId(id);
        order.setTotalSum(sum);
        order.setDateTime(LocalDateTime.now());
        return order;
    }

    private OrderItem createOrderItem(Long orderId, Long itemId, int count, BigDecimal price) {
        OrderItem oi = new OrderItem();
        oi.setOrderId(orderId);
        oi.setItemId(itemId);
        oi.setCount(count);
        oi.setPrice(price);
        return oi;
    }

    private Item createItem(Long id, String title, BigDecimal price) {
        Item item = new Item();
        item.setId(id);
        item.setTitle(title);
        item.setPrice(price);
        return item;
    }

    private ItemDto createItemDto(Long id, String title, BigDecimal price) {
        return ItemDto.builder()
                .id(id)
                .title(title)
                .price(price)
                .build();
    }

    private void verifyOrderDto(OrderDto orderDto, Order order, OrderItem orderItem, ItemDto itemDto) {
        assertEquals(order.getId(), orderDto.id());
        assertEquals(order.getTotalSum(), orderDto.totalSum());
        assertEquals(order.getDateTime(), orderDto.dateTime());
        assertEquals(1, orderDto.items().size());

        OrderItemDto oi = orderDto.items().get(0);

        assertEquals(orderItem.getCount(), oi.count());
        assertEquals(orderItem.getPrice(), oi.price());
        assertEquals(orderItem.getPrice().multiply(BigDecimal.valueOf(orderItem.getCount())), oi.total());

        assertEquals(itemDto.id(), oi.item().id());
        assertEquals(itemDto.title(), oi.item().title());
        assertEquals(itemDto.price(), oi.item().price());
    }
}
