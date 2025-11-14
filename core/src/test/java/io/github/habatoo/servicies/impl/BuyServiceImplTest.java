package io.github.habatoo.servicies.impl;

import io.github.habatoo.entity.Cart;
import io.github.habatoo.entity.CartItem;
import io.github.habatoo.entity.Item;
import io.github.habatoo.repositories.CartItemRepository;
import io.github.habatoo.repositories.CartRepository;
import io.github.habatoo.repositories.OrderItemRepository;
import io.github.habatoo.repositories.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Параметризованные unit-тесты для BuyServiceImpl.
 * Покрывают все граничные случаи: успешная покупка, пустая корзина, несуществующая корзина,
 * ошибки при сохранении заказа и очистке корзины.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тест загрузки BuyServiceImpl")
class BuyServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;

    private BuyServiceImpl buyService;

    @BeforeEach
    void setUp() {
        buyService = new BuyServiceImpl(orderRepository, orderItemRepository, cartRepository, cartItemRepository);
    }

    /**
     * Тест успешной покупки, корзина содержит несколько товаров.
     */
    @ParameterizedTest
    @MethodSource("provideCartsForBuy")
    @DisplayName("Успешная покупка — корзина с разными товарами")
    void buySuccessTest(Cart cart) {
        Long cartId = 1L;
        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));

        buyService.buy(cartId);

        verify(cartItemRepository).deleteAll(cart.getItems());
        verify(cartRepository).save(cart);
        assertEquals(BigDecimal.ZERO, cart.getTotal());
        assertTrue(cart.getItems().isEmpty());
    }

    /**
     * Тест: корзина не найдена — ожидается IllegalStateException.
     */
    @Test
    @DisplayName("Падение при отсутствии корзины — выбрасывает IllegalStateException")
    void buyCartNotFoundTest() {
        Long cartId = 1234L;
        when(cartRepository.findById(cartId)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> buyService.buy(cartId));
    }

    /**
     * Тест: корзина пустая — заказ возможен, итоговая сумма 0.
     */
    @Test
    @DisplayName("Покупка при пустой корзине — создаётся пустой заказ, корзина обнуляется")
    void buyEmptyCartTest() {
        Cart emptyCart = new Cart();
        emptyCart.setItems(new ArrayList<>());
        emptyCart.setTotal(BigDecimal.ZERO);
        Long cartId = 55L;
        when(cartRepository.findById(cartId)).thenReturn(Optional.of(emptyCart));

        buyService.buy(cartId);

        verify(cartItemRepository).deleteAll(emptyCart.getItems());
        verify(cartRepository).save(emptyCart);
        assertTrue(emptyCart.getItems().isEmpty());
        assertEquals(BigDecimal.ZERO, emptyCart.getTotal());
    }

    /**
     * Тест: ошибка при сохранении заказа (имитация проблемы с базой).
     */
    @Test
    @DisplayName("Ошибка БД при сохранении заказа — пробрасывается исключение")
    void buyOrderSaveFailureTest() {
        Long cartId = 22L;
        Cart cart = mock(Cart.class);
        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));
        when(orderRepository.save(any()))
                .thenThrow(new DataAccessResourceFailureException("DB Ошибка"));
        assertThrows(DataAccessResourceFailureException.class, () -> buyService.buy(cartId));
    }

    /**
     * Тест: ошибка при сохранении корзины после оформления — пробрасывается исключение.
     */
    @Test
    @DisplayName("Ошибка при сохранении корзины после покупки — пробрасывается исключение")
    void buyCartSaveFailureTest() {
        Long cartId = 33L;
        Cart cart = new Cart();
        cart.setItems(new ArrayList<>());
        cart.setTotal(BigDecimal.ONE);
        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));
        doThrow(new DataAccessResourceFailureException("DB Ошибка"))
                .when(cartRepository).save(cart);

        assertThrows(DataAccessResourceFailureException.class, () -> buyService.buy(cartId));
    }

    /**
     * Тест: ошибка при поиске корзины после оформления — пробрасывается исключение.
     */
    @Test
    @DisplayName("Ошибка при поиске корзины после покупки — пробрасывается исключение")
    void buyCartFindFailureTest() {
        Long cartId = 33L;
        Cart cart = new Cart();
        cart.setItems(new ArrayList<>());
        cart.setTotal(BigDecimal.ONE);

        doThrow(new IllegalStateException("Корзина с id=%d не найден".formatted(cartId)))
                .when(cartRepository).findById(cartId);
        assertThrows(IllegalStateException.class, () -> buyService.buy(cartId));
    }

    static Stream<Cart> provideCartsForBuy() {
        CartItem item1 = new CartItem();
        item1.setCount(2);
        item1.setPrice(BigDecimal.valueOf(100));
        item1.setItem(new Item());

        CartItem item2 = new CartItem();
        item2.setCount(1);
        item2.setPrice(BigDecimal.valueOf(300));
        item2.setItem(new Item());

        Cart cart1 = new Cart();
        cart1.setItems(new ArrayList<>(List.of(item1, item2)));
        cart1.setTotal(BigDecimal.valueOf(500));

        Cart cart2 = new Cart();
        cart2.setItems(new ArrayList<>(List.of(item1)));
        cart2.setTotal(BigDecimal.valueOf(200));

        return Stream.of(cart1, cart2);
    }
}
