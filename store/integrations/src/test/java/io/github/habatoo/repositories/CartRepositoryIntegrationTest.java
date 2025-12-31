package io.github.habatoo.repositories;

import io.github.habatoo.utils.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Reactive интеграционные тесты CartRepository")
class CartRepositoryIntegrationTest extends BaseTest {

    @Test
    @DisplayName("Сохранение и выборка корзины по id")
    void findSavedCartByIdTest() {
        StepVerifier.create(
                        createAndSaveCart(BigDecimal.valueOf(100))
                                .flatMap(c -> cartRepository.findById(c.getId()))
                )
                .assertNext(cart -> assertEquals(0, cart.getTotal().compareTo(BigDecimal.valueOf(100))))
                .verifyComplete();
    }

    @Test
    @DisplayName("Удаление корзины и проверка, что она не найдена")
    void deleteCartByIdTest() {
        StepVerifier.create(
                        createAndSaveCart(BigDecimal.ZERO)
                                .flatMap(c -> cartRepository.deleteById(c.getId()).thenReturn(c.getId()))
                                .flatMap(id -> cartRepository.findById(id))
                )
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    @DisplayName("Поиск всех корзин")
    void findAllCartsTest() {
        StepVerifier.create(
                        Flux.concat(
                                createAndSaveCart(BigDecimal.valueOf(10)),
                                createAndSaveCart(BigDecimal.valueOf(50))
                        ).thenMany(cartRepository.findAll()).collectList()
                )
                .assertNext(list -> assertEquals(2, list.size()))
                .verifyComplete();
    }
}
