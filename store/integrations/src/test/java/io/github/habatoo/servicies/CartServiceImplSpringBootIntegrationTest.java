package io.github.habatoo.servicies;

import io.github.habatoo.dto.enums.Action;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.utils.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Интеграционные тесты CartServiceImpl")
class CartServiceImplSpringBootIntegrationTest extends BaseTest {

    @Autowired
    private CartService cartService;

    @Test
    @DisplayName("Добавление нового товара в пустую корзину")
    void addItemToEmptyCartTest() {
        String extId = "test-ext-id-1";

        Mono<Void> testChain = createAndSaveItem("Item1", BigDecimal.valueOf(100))
                .flatMap(item -> {
                    ChangeNumberOfItemsRequestDto request = ChangeNumberOfItemsRequestDto.builder()
                            .id(item.getId())
                            .action(Action.PLUS)
                            .build();

                    return cartService.changeNumberOfItems(request)
                            .contextWrite(createSecurityContext(extId, "testUser"))
                            .doOnNext(itemDto -> {
                                assertEquals(itemDto.id(), item.getId());
                                assertEquals(itemDto.title(), item.getTitle());
                            })
                            .then(cartService.getItemsInTheCart())
                            .contextWrite(createSecurityContext(extId, "testUser"))
                            .doOnNext(cartDto -> {
                                assertEquals(1, cartDto.items().size());
                                assertEquals(0, cartDto.total().compareTo(BigDecimal.valueOf(100)));
                            });
                })
                .then();

        StepVerifier.create(testChain)
                .verifyComplete();
    }

    @Test
    @DisplayName("Увеличение количества существующего товара в корзине")
    void increaseItemCountTest() {
        String extId = "test-ext-id-2";

        Mono<Void> testChain = createAndSaveUserWithId(extId, "user2")
                .flatMap(user -> createAndSaveCart(BigDecimal.ZERO, user)
                        .flatMap(cart -> createAndSaveItem("Item2", BigDecimal.valueOf(50))
                                .flatMap(item -> createAndSaveCartItem(cart, item, 1, item.getPrice())
                                        .thenReturn(item))))
                .flatMap(item -> {
                    ChangeNumberOfItemsRequestDto request = ChangeNumberOfItemsRequestDto.builder()
                            .id(item.getId())
                            .action(Action.PLUS)
                            .build();

                    return cartService.changeNumberOfItems(request)
                            .contextWrite(createSecurityContext(extId, "user2"))
                            .then(cartService.getItemsInTheCart())
                            .contextWrite(createSecurityContext(extId, "user2"))
                            .doOnNext(cartDto -> {
                                assertEquals(1, cartDto.items().size());
                                assertEquals(2, (int) cartDto.items().get(0).count());
                                assertEquals(0, cartDto.total().compareTo(BigDecimal.valueOf(100)));
                            });
                })
                .then();

        StepVerifier.create(testChain)
                .verifyComplete();
    }

    @Test
    @DisplayName("Уменьшение количества товара до удаления")
    void decreaseItemCountToZeroTest() {
        String extId = "test-ext-id-3";

        Mono<Void> testChain = createAndSaveUserWithId(extId, "user3")
                .flatMap(user -> createAndSaveCart(BigDecimal.ZERO, user)
                        .flatMap(cart -> createAndSaveItem("Item3", BigDecimal.valueOf(30))
                                .flatMap(item -> createAndSaveCartItem(cart, item, 1, item.getPrice())
                                        .thenReturn(item))))
                .flatMap(item -> {
                    ChangeNumberOfItemsRequestDto request = ChangeNumberOfItemsRequestDto.builder()
                            .id(item.getId())
                            .action(Action.MINUS)
                            .build();

                    return cartService.changeNumberOfItems(request)
                            .contextWrite(createSecurityContext(extId, "user3"))
                            .then(cartService.getItemsInTheCart())
                            .contextWrite(createSecurityContext(extId, "user3"))
                            .doOnNext(cartDto -> {
                                assertTrue(cartDto.items().isEmpty());
                                assertEquals(0, cartDto.total().compareTo(BigDecimal.ZERO));
                            });
                })
                .then();

        StepVerifier.create(testChain)
                .verifyComplete();
    }

    @Test
    @DisplayName("Попытка уменьшить количество товара, которого нет в корзине")
    void decreaseNonExistingItemTest() {
        ChangeNumberOfItemsRequestDto request = ChangeNumberOfItemsRequestDto.builder()
                .id(999L)
                .action(Action.MINUS)
                .build();

        StepVerifier.create(cartService.changeNumberOfItems(request)
                        .contextWrite(createSecurityContext("any", "user")))
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Добавление товара: возвращает пустой Mono для неавторизованного пользователя")
    void addItemUnauthenticatedTest() {
        ChangeNumberOfItemsRequestDto request = ChangeNumberOfItemsRequestDto.builder()
                .id(1L)
                .action(Action.PLUS)
                .build();

        StepVerifier.create(cartService.changeNumberOfItems(request))
                .verifyComplete();
    }

    @Test
    @DisplayName("Получение корзины: возвращает пустой Mono для неавторизованного пользователя")
    void getItemsUnauthenticatedTest() {
        StepVerifier.create(cartService.getItemsInTheCart())
                .verifyComplete();
    }

    @Test
    @DisplayName("Изменение количества из корзины: возвращает пустой Mono для неавторизованного пользователя")
    void changeItemsFromCartUnauthenticatedTest() {
        ChangeNumberOfItemsRequestDto request = ChangeNumberOfItemsRequestDto.builder()
                .id(1L)
                .action(Action.PLUS)
                .build();

        StepVerifier.create(cartService.changeNumberOfItemsFromCart(request))
                .verifyComplete();
    }
}
