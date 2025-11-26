package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.response.ItemDto;
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
    @DisplayName("getOrders — заказы с позициями")
    void getOrdersTest() {
        Order order = new Order();
        order.setId(1L);
        order.setTotalSum(BigDecimal.valueOf(100));
        order.setDateTime(LocalDateTime.now());

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(1L);
        orderItem.setItemId(10L);
        orderItem.setCount(2);
        orderItem.setPrice(BigDecimal.valueOf(50));

        Item item = new Item();
        item.setId(10L);
        item.setTitle("Item 10");
        item.setPrice(BigDecimal.valueOf(50));

        when(orderRepository.findAll()).thenReturn(Flux.just(order));
        when(orderItemRepository.findAllByOrderId(1L)).thenReturn(Flux.just(orderItem));
        when(itemRepository.findById(10L)).thenReturn(Mono.just(item));
        when(mapper.toDto(item)).thenReturn(
                ItemDto.builder().id(10L).title("Item 10").price(BigDecimal.valueOf(50)).build());

        StepVerifier.create(service.getOrders())
                .assertNext(orderDto -> {
                    assertEquals(order.getId(), orderDto.id());
                    assertEquals(order.getTotalSum(), orderDto.totalSum());
                    assertEquals(order.getDateTime(), orderDto.dateTime());
                    assertEquals(1, orderDto.items().size());

                    var orderItemDto = orderDto.items().get(0);
                    assertEquals(orderItem.getCount(), orderItemDto.count());
                    assertEquals(orderItem.getPrice(), orderItemDto.price());
                    assertEquals(orderItem.getPrice().multiply(BigDecimal.valueOf(
                            orderItem.getCount())), orderItemDto.total());
                    assertEquals(item.getId(), orderItemDto.item().id());
                    assertEquals(item.getTitle(), orderItemDto.item().title());
                    assertEquals(item.getPrice(), orderItemDto.item().price());
                })
                .verifyComplete();

        verify(orderRepository).findAll();
        verify(orderItemRepository).findAllByOrderId(1L);
        verify(itemRepository).findById(10L);
    }

    /**
     * Тест получения конкретного заказа — найден.
     */
    @Test
    @DisplayName("getOrder — заказ найден")
    void getOrderFoundTest() {
        Order order = new Order();
        order.setId(2L);
        order.setTotalSum(BigDecimal.valueOf(200));
        order.setDateTime(LocalDateTime.now());

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(2L);
        orderItem.setItemId(20L);
        orderItem.setCount(1);
        orderItem.setPrice(BigDecimal.valueOf(200));

        Item item = new Item();
        item.setId(20L);
        item.setTitle("Item 20");
        item.setPrice(BigDecimal.valueOf(200));

        when(orderRepository.findById(2L)).thenReturn(Mono.just(order));
        when(orderItemRepository.findAllByOrderId(2L)).thenReturn(Flux.just(orderItem));
        when(itemRepository.findById(20L)).thenReturn(Mono.just(item));
        when(mapper.toDto(item)).thenReturn(ItemDto.builder().build());

        StepVerifier.create(service.getOrder(2L, true))
                .assertNext(orderDto -> {
                    assertEquals(order.getId(), orderDto.id());
                    assertEquals(1, orderDto.items().size());
                    assertEquals(order.getTotalSum(), orderDto.totalSum());
                })
                .verifyComplete();

        verify(orderRepository).findById(2L);
        verify(orderItemRepository).findAllByOrderId(2L);
        verify(itemRepository).findById(20L);
    }

    /**
     * Тест получения заказа — не найден.
     */
    @Test
    @DisplayName("getOrder — заказ не найден, выбрасывает исключение")
    void getOrderNotFoundTest() {
        when(orderRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(service.getOrder(99L, true))
                .expectErrorMatches(err -> err instanceof IllegalStateException
                        && err.getMessage().equals("Заказ с id=99 не найден"))
                .verify();

        verify(orderRepository).findById(99L);
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
}
