package io.github.habatoo.repositories;

import io.github.habatoo.entity.CartItem;
import io.github.habatoo.utils.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты для CartItemRepository.
 * Проверяют сохранение, поиск, удаление позиций корзины, а также обработку невалидных связей.
 */
@DataR2dbcTest
@DisplayName("Интеграционные тесты CartItemRepository (R2DBC)")
class CartItemRepositoryIntegrationTest extends BaseTest {

    @Test
    @DisplayName("Сохранение CartItem и поиск по id")
    void findSavedCartItemByIdTest() {
        Mono<CartItem> testFlow =
                createAndSaveCart()
                        .zipWith(createAndSaveItem("CartItem1", BigDecimal.valueOf(15)))
                        .flatMap(tuple -> createAndSaveCartItem(
                                tuple.getT1(),
                                tuple.getT2(),
                                2,
                                BigDecimal.valueOf(15)))
                        .flatMap(saved -> cartItemRepository.findById(saved.getId())
                                .map(found -> {
                                    assertThat(found).isNotNull();
                                    assertThat(found.getCartId()).isEqualTo(saved.getCartId());
                                    assertThat(found.getItemId()).isEqualTo(saved.getItemId());
                                    assertThat(found.getCount()).isEqualTo(2);
                                    assertThat(found.getPrice()).isEqualTo(BigDecimal.valueOf(15));
                                    return found;
                                })
                        );

        StepVerifier.create(testFlow).expectNextCount(1).verifyComplete();
    }

    @Test
    @DisplayName("Сохранение нескольких CartItem и поиск всех")
    void findAllCartItemsTest() {
        Mono<List<CartItem>> testFlow =
                createAndSaveCart()
                        .zipWith(createAndSaveItem("ItemA", BigDecimal.ONE))
                        .flatMap(tuple -> createAndSaveCartItem(
                                tuple.getT1(), tuple.getT2(), 1, BigDecimal.ONE))
                        .then(
                                createAndSaveCart()
                                        .zipWith(createAndSaveItem("ItemB", BigDecimal.TEN))
                                        .flatMap(tuple -> createAndSaveCartItem(
                                                tuple.getT1(), tuple.getT2(), 3, BigDecimal.TEN))
                        )
                        .then(cartItemRepository.findAll().collectList())
                        .doOnNext(list -> assertThat(list).hasSize(2));

        StepVerifier.create(testFlow).expectNextCount(1).verifyComplete();
    }

    @Test
    @DisplayName("Сохранение CartItem с валидной связкой Cart и Item")
    void createCartItemWithValidRelationsTest() {
        Mono<CartItem> testFlow =
                createAndSaveCart(BigDecimal.valueOf(10))
                        .zipWith(createAndSaveItem("CartBindItem", BigDecimal.valueOf(10)))
                        .flatMap(tuple -> createAndSaveCartItem(
                                tuple.getT1(), tuple.getT2(), 2, BigDecimal.valueOf(10)))
                        .flatMap(saved -> cartItemRepository.findById(saved.getId())
                                .map(found -> {
                                    assertThat(found).isNotNull();
                                    assertThat(found.getCartId()).isEqualTo(saved.getCartId());
                                    assertThat(found.getItemId()).isEqualTo(saved.getItemId());
                                    assertThat(found.getCount()).isEqualTo(2);
                                    assertThat(found.getPrice()).isEqualTo(BigDecimal.valueOf(10));
                                    return found;
                                })
                        );

        StepVerifier.create(testFlow).expectNextCount(1).verifyComplete();
    }

    @Test
    @DisplayName("Добавление нескольких CartItem и проверка связи с корзиной")
    void createMultipleItemsAndCartRelationTest() {
        Mono<List<CartItem>> testFlow =
                createAndSaveCart(BigDecimal.valueOf(20))
                        .flatMap(cart ->
                                createAndSaveItem("A", BigDecimal.ONE)
                                        .zipWith(createAndSaveItem("B", BigDecimal.TEN))
                                        .flatMap(tuple -> createAndSaveCartItem(
                                                cart, tuple.getT1(), 3, BigDecimal.ONE)
                                                .then(createAndSaveCartItem(
                                                        cart, tuple.getT2(), 5, BigDecimal.TEN))
                                        )
                                        .then(cartItemRepository.findAll().collectList())
                                        .doOnNext(items -> {
                                            assertThat(items).hasSize(2);
                                            assertThat(items.stream()
                                                    .map(CartItem::getCartId)
                                                    .distinct().toList()
                                            ).containsOnly(cart.getId());
                                        })
                        );

        StepVerifier.create(testFlow).expectNextCount(1).verifyComplete();
    }

    @Test
    @DisplayName("Поиск CartItem по несуществующему id возвращает empty Mono")
    void findCartItemByNonExistingIdTest() {
        StepVerifier.create(cartItemRepository.findById(-7777L))
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Создание CartItem без cartId вызывает ошибку (NOT NULL violation)")
    void createCartItemWithoutCartTest() {
        Mono<CartItem> testFlow =
                createAndSaveItem("NoCart", BigDecimal.valueOf(9))
                        .flatMap(item -> {
                            CartItem ci = new CartItem();
                            ci.setItemId(item.getId());
                            ci.setCount(1);
                            ci.setPrice(BigDecimal.valueOf(9));
                            return cartItemRepository.save(ci);
                        });

        StepVerifier.create(testFlow)
                .expectError()
                .verify();
    }

    @Test
    @DisplayName("Создание CartItem без itemId вызывает ошибку (NOT NULL violation)")
    void createCartItemWithoutItemTest() {
        Mono<CartItem> testFlow =
                createAndSaveCart(BigDecimal.valueOf(5))
                        .flatMap(cart -> {
                            CartItem ci = new CartItem();
                            ci.setCartId(cart.getId());
                            ci.setCount(1);
                            ci.setPrice(BigDecimal.valueOf(5));
                            return cartItemRepository.save(ci);
                        });

        StepVerifier.create(testFlow)
                .expectError()
                .verify();
    }

    @Test
    @DisplayName("Удаление CartItem по id")
    void deleteCartItemByIdTest() {
        Mono<Void> testFlow =
                createAndSaveCart(BigDecimal.ONE)
                        .zipWith(createAndSaveItem("ToDelete", BigDecimal.ONE))
                        .flatMap(tuple -> createAndSaveCartItem(
                                tuple.getT1(), tuple.getT2(), 1, BigDecimal.ONE))
                        .flatMap(saved ->
                                cartItemRepository.deleteById(saved.getId())
                                        .then(cartItemRepository.findById(saved.getId()))
                                        .doOnNext(found -> assertThat(found).isNull())
                        )
                        .then();

        StepVerifier.create(testFlow).verifyComplete();
    }

    @Test
    @DisplayName("findCountByCartIdAndItemId возвращает количество для пары cartId/itemId")
    void whenCartItemExists_returnsCount() {
        int expectedCount = 8;

        Mono<Integer> testFlow =
                createAndSaveCart(BigDecimal.TEN)
                        .zipWith(createAndSaveItem("CartBindItem", BigDecimal.TEN))
                        .flatMap(tuple -> createAndSaveCartItem(
                                tuple.getT1(), tuple.getT2(), expectedCount, BigDecimal.TEN)
                                .thenReturn(tuple))
                        .flatMap(tuple -> cartItemRepository.findCountByCartIdAndItemId(
                                tuple.getT1().getId(), tuple.getT2().getId()))
                        .doOnNext(count -> assertThat(count).isEqualTo(expectedCount));

        StepVerifier.create(testFlow).expectNextCount(1).verifyComplete();
    }

    @Test
    @DisplayName("findCountByCartIdAndItemId возвращает empty если записи нет")
    void whenCartItemNotExists_returnsNull() {
        StepVerifier.create(cartItemRepository.findCountByCartIdAndItemId(-123L, -456L))
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("deleteAllByCartId удаляет все записи корзины")
    void deleteAllByCartIdTest() {
        Mono<Void> testFlow =
                createAndSaveCart()
                        .zipWith(createAndSaveItem("X", BigDecimal.ONE))
                        .flatMap(tuple -> createAndSaveCartItem(
                                tuple.getT1(), tuple.getT2(), 1, BigDecimal.ONE)
                                .thenReturn(tuple.getT1()))
                        .flatMap(cart -> cartItemRepository.deleteAllByCartId(cart.getId())
                                .then(cartItemRepository.findAllByCartId(cart.getId()).collectList())
                                .doOnNext(list -> assertThat(list).isEmpty())
                                .then()
                        );

        StepVerifier.create(testFlow).verifyComplete();
    }
}
