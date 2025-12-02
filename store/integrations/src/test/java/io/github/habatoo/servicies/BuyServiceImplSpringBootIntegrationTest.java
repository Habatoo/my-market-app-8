package io.github.habatoo.servicies;

import io.github.habatoo.Application;
import io.github.habatoo.entity.Cart;
import io.github.habatoo.entity.Item;
import io.github.habatoo.entity.Order;
import io.github.habatoo.utils.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.AutoConfigureDataR2dbc;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = Application.class)
@AutoConfigureDataR2dbc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Интеграционный тест BuyServiceImpl")
class BuyServiceImplSpringBootIntegrationTest extends BaseTest {

    @Autowired
    private BuyService buyService;

    @Test
    @DisplayName("Успешная покупка корзины с товарами")
    void buyCartSuccessfullyTest() {
        Cart cart = createAndSaveCart(BigDecimal.valueOf(100)).block();

        Item item1 = createAndSaveItem("Item1", BigDecimal.valueOf(10)).block();
        Item item2 = createAndSaveItem("Item2", BigDecimal.valueOf(20)).block();

        createAndSaveCartItem(cart, item1, 2, item1.getPrice()).block();
        createAndSaveCartItem(cart, item2, 3, item2.getPrice()).block();

        StepVerifier.create(buyService.buy(cart.getId()))
                .expectNextMatches(orderId -> orderId != null && orderId > 0)
                .verifyComplete();

        StepVerifier.create(cartRepository.findById(cart.getId()))
                .assertNext(c -> assertEquals(0, c.getTotal().compareTo(BigDecimal.ZERO)))
                .verifyComplete();

        StepVerifier.create(cartItemRepository.findAllByCartId(cart.getId()).collectList())
                .expectNextMatches(List::isEmpty)
                .verifyComplete();

        StepVerifier.create(orderRepository.findAll().collectList())
                .assertNext(orders -> {
                    assert orders.size() == 1;
                    Order order = orders.get(0);
                    BigDecimal expectedTotal = item1.getPrice().multiply(BigDecimal.valueOf(2))
                            .add(item2.getPrice().multiply(BigDecimal.valueOf(3)));
                    assert order.getTotalSum().compareTo(expectedTotal) == 0;
                })
                .verifyComplete();

        StepVerifier.create(orderItemRepository.findAll().collectList())
                .assertNext(orderItems -> {
                    assert orderItems.size() == 2;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Попытка покупки несуществующей корзины")
    void buyNonExistingCartTest() {
        StepVerifier.create(buyService.buy(-1L))
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && e.getMessage().contains("Корзина с id=-1 не найдена"))
                .verify();
    }

    @Test
    @DisplayName("Попытка покупки пустой корзины")
    void buyEmptyCartTest() {
        Cart cart = createAndSaveCart(BigDecimal.valueOf(0)).block();

        StepVerifier.create(buyService.buy(cart.getId()))
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && e.getMessage().contains("В корзине нет товаров для покупки"))
                .verify();
    }
}
