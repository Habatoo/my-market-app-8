package io.github.habatoo.servicies;

import io.github.habatoo.dto.enums.Action;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.entity.Cart;
import io.github.habatoo.entity.Item;
import io.github.habatoo.utils.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.AutoConfigureDataR2dbc;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureDataR2dbc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Интеграционные тесты CartServiceImpl")
class CartServiceImplSpringBootIntegrationTest extends BaseTest {

    @Autowired
    private CartService cartService;

    @Test
    @DisplayName("Добавление нового товара в пустую корзину")
    void addItemToEmptyCartTest() {
        Item item = createAndSaveItem("Item1", BigDecimal.valueOf(100)).block();

        ChangeNumberOfItemsRequestDto request = ChangeNumberOfItemsRequestDto.builder()
                .id(item.getId())
                .action(Action.PLUS)
                .build();

        StepVerifier.create(cartService.changeNumberOfItems(request))
                .assertNext(itemDto -> {
                    assertEquals(itemDto.id(), item.getId());
                    assertEquals(itemDto.title(), item.getTitle());
                })
                .verifyComplete();

        StepVerifier.create(cartService.getItemsInTheCart())
                .assertNext(cartDto -> {
                    assert cartDto.items().size() == 1;
                    assert cartDto.total().compareTo(BigDecimal.valueOf(100)) == 0;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Увеличение количества существующего товара в корзине")
    void increaseItemCountTest() {
        Cart cart = createAndSaveCart().block();
        Item item = createAndSaveItem("Item2", BigDecimal.valueOf(50)).block();
        createAndSaveCartItem(cart, item, 1, item.getPrice()).block();

        ChangeNumberOfItemsRequestDto request = ChangeNumberOfItemsRequestDto.builder()
                .id(item.getId())
                .action(Action.PLUS)
                .build();

        StepVerifier.create(cartService.changeNumberOfItems(request))
                .assertNext(itemDto -> assertEquals(itemDto.id(), item.getId()))
                .verifyComplete();

        StepVerifier.create(cartService.getItemsInTheCart())
                .assertNext(cartDto -> {
                    assertEquals(2, (int) cartDto.items().get(0).count());
                    assertEquals(0, cartDto.total().compareTo(BigDecimal.valueOf(100)));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Уменьшение количества товара до удаления")
    void decreaseItemCountToZeroTest() {
        Cart cart = createAndSaveCart().block();
        Item item = createAndSaveItem("Item3", BigDecimal.valueOf(30)).block();
        createAndSaveCartItem(cart, item, 1, item.getPrice()).block();

        ChangeNumberOfItemsRequestDto request = ChangeNumberOfItemsRequestDto.builder()
                .id(item.getId())
                .action(Action.MINUS)
                .build();

        StepVerifier.create(cartService.changeNumberOfItems(request))
                .assertNext(itemDto -> {
                    assertEquals(itemDto.id(), item.getId());
                    assertEquals(0, (int) itemDto.count());
                })
                .verifyComplete();

        StepVerifier.create(cartService.getItemsInTheCart())
                .assertNext(cartDto -> {
                    assertTrue(cartDto.items().isEmpty());
                    assertEquals(0, cartDto.total().compareTo(BigDecimal.ZERO));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Попытка уменьшить количество товара, которого нет в корзине")
    void decreaseNonExistingItemTest() {
        ChangeNumberOfItemsRequestDto request = ChangeNumberOfItemsRequestDto.builder()
                .id(999L)
                .action(Action.MINUS)
                .build();

        StepVerifier.create(cartService.changeNumberOfItems(request))
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Получение полной корзины с несколькими товарами")
    void getCartWithMultipleItemsTest() {
        Cart cart = createAndSaveCart().block();
        Item item1 = createAndSaveItem("ItemA", BigDecimal.valueOf(10)).block();
        Item item2 = createAndSaveItem("ItemB", BigDecimal.valueOf(20)).block();

        createAndSaveCartItem(cart, item1, 2, item1.getPrice()).block();
        createAndSaveCartItem(cart, item2, 1, item2.getPrice()).block();

        StepVerifier.create(cartService.getItemsInTheCart())
                .assertNext(cartDto -> {
                    assertEquals(2, cartDto.items().size());
                    BigDecimal expectedTotal = item1.getPrice().multiply(BigDecimal.valueOf(2))
                            .add(item2.getPrice());
                    assertEquals(0, cartDto.total().compareTo(expectedTotal));
                })
                .verifyComplete();
    }
}
