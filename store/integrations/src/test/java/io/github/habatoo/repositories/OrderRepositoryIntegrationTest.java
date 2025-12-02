package io.github.habatoo.repositories;

import io.github.habatoo.utils.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Интеграционные тесты для реактивного OrderRepository.
 */
@DataR2dbcTest
@DisplayName("Reactive интеграционные тесты OrderRepository")
class OrderRepositoryIntegrationTest extends BaseTest {

    @Test
    @DisplayName("Сохранение и поиск заказа по id")
    void findSavedOrderByIdTest() {
        LocalDateTime now = LocalDateTime.now();
        StepVerifier.create(
                        createAndSaveOrder(BigDecimal.valueOf(111), now)
                                .flatMap(order -> orderRepository.findById(order.getId()))
                )
                .assertNext(
                        order -> assertEquals(0, order.getTotalSum().compareTo(BigDecimal.valueOf(111))))
                .verifyComplete();
    }

    @Test
    @DisplayName("Удаление заказа по id")
    void deleteOrderByIdTest() {
        StepVerifier.create(
                        createAndSaveOrder(BigDecimal.ZERO, LocalDateTime.now())
                                .flatMap(o -> orderRepository.deleteById(o.getId()).thenReturn(o.getId()))
                                .flatMap(id -> orderRepository.findById(id))
                )
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    @DisplayName("Поиск всех заказов")
    void findAllOrdersTest() {
        StepVerifier.create(
                        Flux.concat(
                                createAndSaveOrder(BigDecimal.valueOf(100), LocalDateTime.now()),
                                createAndSaveOrder(BigDecimal.valueOf(200), LocalDateTime.now())
                        ).thenMany(orderRepository.findAll()).collectList()
                )
                .assertNext(list -> assertEquals(2, list.size()))
                .verifyComplete();
    }
}
