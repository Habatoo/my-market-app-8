package io.github.habatoo.repositories;

import io.github.habatoo.entity.Item;
import io.github.habatoo.entity.Order;
import io.github.habatoo.entity.OrderItem;
import io.github.habatoo.utils.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Reactive интеграционные тесты OrderItem - Order - Item связей")
class OrderItemRepositoryIntegrationTest extends BaseTest {

    @Test
    @DisplayName("Создание OrderItem с валидными Order и Item")
    void createOrderItemWithValidRelationsTest() {

        Mono<OrderItem> flow = createAndSaveItem("OrderBindItem", BigDecimal.valueOf(19))
                .zipWith(createAndSaveOrder(BigDecimal.valueOf(19), LocalDateTime.now()))
                .flatMap(tuple -> {
                    Item item = tuple.getT1();
                    Order order = tuple.getT2();
                    return createAndSaveOrderItem(order, item, 3, BigDecimal.valueOf(19));
                })
                .flatMap(saved -> orderItemRepository.findById(saved.getId()));

        StepVerifier.create(flow)
                .assertNext(found -> {
                    assertThat(found.getOrderId()).isNotNull();
                    assertThat(found.getItemId()).isNotNull();
                    assertThat(found.getCount()).isEqualTo(3);
                    assertThat(found.getPrice()).isEqualTo(BigDecimal.valueOf(19));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Добавление нескольких OrderItem и проверка связи с заказом")
    void createMultipleItemsAndOrderRelationTest() {

        Mono<Void> setup = createAndSaveOrder(BigDecimal.valueOf(35), LocalDateTime.now())
                .flatMap(order ->
                        createAndSaveItem("A", BigDecimal.valueOf(10))
                                .flatMap(item1 -> createAndSaveOrderItem(order, item1, 1, BigDecimal.valueOf(10)))
                                .then(createAndSaveItem("B", BigDecimal.valueOf(25)))
                                .flatMap(item2 -> createAndSaveOrderItem(order, item2, 2, BigDecimal.valueOf(25)))
                )
                .then();

        StepVerifier.create(
                        setup.thenMany(orderItemRepository.findAll().collectList())
                )
                .assertNext(list -> {
                    assertThat(list).hasSize(2);
                    assertThat(
                            list.stream()
                                    .map(OrderItem::getOrderId)
                                    .distinct()
                    ).containsOnly(list.get(0).getOrderId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Поиск OrderItem по несуществующему id")
    void findOrderItemByNonExistingIdTest() {
        StepVerifier.create(orderItemRepository.findById(-12345L))
                .verifyComplete();
    }

    @Test
    @DisplayName("Создание OrderItem без Order вызывает ошибку")
    void createOrderItemWithoutOrderTest() {

        Mono<OrderItem> flow = createAndSaveItem("NoOrder", BigDecimal.valueOf(12))
                .flatMap(item -> {
                    OrderItem oi = new OrderItem();
                    oi.setItemId(item.getId());
                    oi.setCount(1);
                    oi.setPrice(BigDecimal.valueOf(12));
                    return orderItemRepository.save(oi);
                });

        StepVerifier.create(flow)
                .expectError()
                .verify();
    }

    @Test
    @DisplayName("Создание OrderItem без Item вызывает ошибку")
    void createOrderItemWithoutItemTest() {

        Mono<OrderItem> flow = createAndSaveOrder(BigDecimal.ONE, LocalDateTime.now())
                .flatMap(order -> {
                    OrderItem oi = new OrderItem();
                    oi.setOrderId(order.getId());
                    oi.setCount(1);
                    oi.setPrice(BigDecimal.ONE);
                    return orderItemRepository.save(oi);
                });

        StepVerifier.create(flow)
                .expectError()
                .verify();
    }

    @Test
    @DisplayName("Удаление OrderItem по id")
    void deleteOrderItemByIdTest() {

        Mono<Boolean> testFlow = createAndSaveItem("ToDelete", BigDecimal.valueOf(8))
                .zipWith(createAndSaveOrder(BigDecimal.valueOf(8), LocalDateTime.now()))
                .flatMap(tuple -> {
                    Item item = tuple.getT1();
                    Order order = tuple.getT2();
                    return createAndSaveOrderItem(order, item, 1, BigDecimal.valueOf(8));
                })
                .flatMap(saved ->
                        orderItemRepository.deleteById(saved.getId())
                                .then(orderItemRepository.findById(saved.getId()))
                )
                .map(x -> true)
                .defaultIfEmpty(false);

        StepVerifier.create(testFlow)
                .expectNext(false)
                .verifyComplete();
    }
}
