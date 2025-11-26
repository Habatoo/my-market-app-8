package io.github.habatoo.repositories;

import io.github.habatoo.utils.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataR2dbcTest
@DisplayName("Reactive интеграционные тесты ItemRepository")
class ItemRepositoryIntegrationTest extends BaseTest {

    @Test
    @DisplayName("Поиск сохранённого товара по id")
    void saveItemHaveToBeFoundTest() {
        StepVerifier.create(
                        createAndSaveItem("Title_1", BigDecimal.TEN)
                                .flatMap(item -> itemRepository.findById(item.getId()))
                )
                .assertNext(item -> {
                    assertEquals("Title_1", item.getTitle());
                    assertEquals(0, item.getPrice().compareTo(BigDecimal.TEN));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Поиск всех товаров")
    void allItemsFoundTest() {
        StepVerifier.create(
                        Flux.concat(
                                createAndSaveItem("Title_1", BigDecimal.TEN),
                                createAndSaveItem("Title_2", BigDecimal.ONE)
                        ).thenMany(itemRepository.findAll()).collectList()
                )
                .assertNext(list -> assertEquals(2, list.size()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Удаление товара по id")
    void deleteItemHaveToBeNotFoundTest() {
        StepVerifier.create(
                        createAndSaveItem("Title_1", BigDecimal.TEN)
                                .flatMap(item -> itemRepository.deleteById(item.getId()).thenReturn(item.getId()))
                                .flatMap(id -> itemRepository.findById(id))
                )
                .expectNextCount(0)
                .verifyComplete();
    }
}
