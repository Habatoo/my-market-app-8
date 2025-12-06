package io.github.habatoo.servicies.impl;

import io.github.habatoo.entity.Cart;
import io.github.habatoo.entity.CartItem;
import io.github.habatoo.entity.Order;
import io.github.habatoo.entity.OrderItem;
import io.github.habatoo.exceptions.PaymentException;
import io.github.habatoo.repositories.CartItemRepository;
import io.github.habatoo.repositories.CartRepository;
import io.github.habatoo.repositories.OrderItemRepository;
import io.github.habatoo.repositories.OrderRepository;
import io.github.habatoo.store.payment.api.PaymentsApi;
import io.github.habatoo.store.payment.model.PaymentResponse;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
    @Mock
    private PaymentsApi paymentsApi;
    @InjectMocks
    private BuyServiceImpl buyService;

    /**
     * Успешная покупка: корзина найдена, товары есть,
     * заказ сохраняется, создаются OrderItem, корзина очищается, total обнуляется.
     */
    @Test
    @DisplayName("buy() — успешная покупка (полный сценарий)")
    void testBuySuccess() {
        long cartId = 100L;

        Cart cart = new Cart();
        cart.setId(cartId);
        cart.setTotal(BigDecimal.valueOf(500));

        CartItem ci1 = new CartItem();
        ci1.setId(1L);
        ci1.setCartId(cartId);
        ci1.setItemId(10L);
        ci1.setCount(2);
        ci1.setPrice(BigDecimal.valueOf(100));

        CartItem ci2 = new CartItem();
        ci2.setId(2L);
        ci2.setCartId(cartId);
        ci2.setItemId(20L);
        ci2.setCount(3);
        ci2.setPrice(BigDecimal.valueOf(50));

        List<CartItem> items = List.of(ci1, ci2);

        Order savedOrder = new Order();
        savedOrder.setId(999L);
        savedOrder.setDateTime(LocalDateTime.now());
        savedOrder.setTotalSum(BigDecimal.valueOf(2 * 100 + 3 * 50));

        when(cartRepository.findById(cartId)).thenReturn(Mono.just(cart));
        when(cartItemRepository.findAllByCartId(cartId)).thenReturn(Flux.fromIterable(items));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(savedOrder));
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(Mono.just(new OrderItem()));
        when(cartItemRepository.deleteAllByCartId(cartId)).thenReturn(Mono.empty());
        when(paymentsApi.createPayment(anyString(), any()))
                .thenReturn(Mono.just(new PaymentResponse().status(PaymentResponse.StatusEnum.SUCCESS)));

        Cart updatedCart = new Cart();
        updatedCart.setId(cartId);
        updatedCart.setTotal(BigDecimal.ZERO);

        AtomicInteger counter = new AtomicInteger(0);
        when(cartRepository.findById(cartId))
                .thenAnswer(invocation -> {
                    if (counter.getAndIncrement() == 0) {
                        return Mono.just(cart);
                    } else {
                        return Mono.just(updatedCart);
                    }
                });

        when(cartRepository.save(any(Cart.class))).thenReturn(Mono.just(updatedCart));

        StepVerifier.create(buyService.buy(cartId))
                .expectNext(savedOrder.getId())
                .verifyComplete();

        verify(cartRepository, times(2)).findById(cartId);
        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository, times(2)).save(any(OrderItem.class));
        verify(cartItemRepository).deleteAllByCartId(cartId);
        verify(cartRepository).save(argThat(c -> c.getTotal().compareTo(BigDecimal.ZERO) == 0));
    }

    /**
     * Ошибка: корзина не найдена по id.
     */
    @Test
    @DisplayName("buy() — корзина не найдена")
    void testBuyCartNotFound() {
        long cartId = 123;

        when(cartRepository.findById(cartId))
                .thenReturn(Mono.empty());

        StepVerifier.create(buyService.buy(cartId))
                .expectErrorMatches(err ->
                        err instanceof IllegalStateException &&
                                err.getMessage().equals("Корзина с id=" + cartId + " не найдена")
                )
                .verify();

        verify(orderRepository, never()).save(any());
    }

    /**
     * Ошибка: корзина найдена, но товаров в ней нет.
     */
    @Test
    @DisplayName("buy() — корзина найдена, но пуста (нет товаров)")
    void testBuyCartEmpty() {
        long cartId = 100;

        Cart cart = new Cart();
        cart.setId(cartId);

        when(cartRepository.findById(cartId)).thenReturn(Mono.just(cart));
        when(cartItemRepository.findAllByCartId(cartId)).thenReturn(Flux.empty());

        StepVerifier.create(buyService.buy(cartId))
                .expectErrorMatches(err ->
                        err instanceof IllegalStateException &&
                                err.getMessage().equals("В корзине нет товаров для покупки")
                )
                .verify();

        verify(orderRepository, never()).save(any());
        verify(orderItemRepository, never()).save(any());
    }

    /**
     * Ошибка сохранения заказа.
     */
    @Test
    @DisplayName("buy() — ошибка сохранения Order")
    void testBuyOrderSaveFails() {
        long cartId = 1;
        Cart cart = new Cart();
        cart.setId(cartId);

        CartItem item = new CartItem();
        item.setId(10L);
        item.setCartId(cartId);
        item.setCount(1);
        item.setPrice(BigDecimal.valueOf(100));

        when(cartRepository.findById(cartId)).thenReturn(Mono.just(cart));
        when(cartItemRepository.findAllByCartId(cartId)).thenReturn(Flux.just(item));
        when(orderRepository.save(any())).thenReturn(Mono.error(new RuntimeException("save error")));
        when(paymentsApi.createPayment(anyString(), any()))
                .thenReturn(Mono.just(new PaymentResponse().status(PaymentResponse.StatusEnum.SUCCESS)));

        StepVerifier.create(buyService.buy(cartId))
                .expectErrorMatches(err -> err.getMessage().equals("save error"))
                .verify();

        verify(orderItemRepository, never()).save(any());
    }

    /**
     * Ошибка при сохранении OrderItem.
     */
    @Test
    @DisplayName("buy() — ошибка сохранения OrderItem должна прерывать процесс и возвращать ошибку")
    void testBuyOrderItemSaveFails() {
        long cartId = 5;

        Cart cart = new Cart();
        cart.setId(cartId);

        CartItem ci = new CartItem();
        ci.setCartId(cartId);
        ci.setItemId(20L);
        ci.setCount(1);
        ci.setPrice(BigDecimal.valueOf(150));

        Order order = new Order();
        order.setId(999L);

        when(cartRepository.findById(cartId)).thenReturn(Mono.just(cart));
        when(cartItemRepository.findAllByCartId(cartId)).thenReturn(Flux.just(ci));
        when(orderRepository.save(any())).thenReturn(Mono.just(order));
        when(paymentsApi.createPayment(anyString(), any()))
                .thenReturn(Mono.just(new PaymentResponse().status(PaymentResponse.StatusEnum.SUCCESS)));

        StepVerifier.create(buyService.buy(cartId))
                .expectError(RuntimeException.class)
                .verify();

        verify(cartItemRepository, atMostOnce()).deleteAllByCartId(any());
    }

    /**
     * Ошибка: не удалось обнулить total корзины.
     */
    @Test
    @DisplayName("buy() — ошибка сохранения корзины после очистки")
    void testBuyErrorWhenSavingUpdatedCart() {
        long cartId = 50;

        Cart cart = new Cart();
        cart.setId(cartId);

        CartItem ci = new CartItem();
        ci.setCartId(cartId);
        ci.setCount(1);
        ci.setPrice(BigDecimal.valueOf(200));

        Order order = new Order();
        order.setId(111L);

        when(cartRepository.findById(cartId)).thenAnswer(invocation -> Mono.just(cart));
        when(cartItemRepository.findAllByCartId(cartId)).thenReturn(Flux.just(ci));
        when(orderRepository.save(any())).thenReturn(Mono.just(order));
        when(orderItemRepository.save(any())).thenReturn(Mono.just(new OrderItem()));
        when(cartItemRepository.deleteAllByCartId(cartId)).thenReturn(Mono.empty());
        when(cartRepository.save(any())).thenReturn(Mono.error(new RuntimeException("cart save error")));
        when(paymentsApi.createPayment(anyString(), any()))
                .thenReturn(Mono.just(new PaymentResponse().status(PaymentResponse.StatusEnum.SUCCESS)));

        StepVerifier.create(buyService.buy(cartId))
                .expectErrorMatches(err -> err.getMessage().equals("cart save error"))
                .verify();
    }

    @Test
    @DisplayName("buy() — недостаточно средств")
    void testInsufficientFunds() {
        long cartId = 10;

        Cart cart = new Cart();
        cart.setId(cartId);

        CartItem ci = new CartItem();
        ci.setCartId(cartId);
        ci.setCount(1);
        ci.setPrice(BigDecimal.valueOf(100));

        when(cartRepository.findById(cartId)).thenReturn(Mono.just(cart));
        when(cartItemRepository.findAllByCartId(cartId)).thenReturn(Flux.just(ci));

        when(paymentsApi.createPayment(anyString(), any()))
                .thenReturn(Mono.just(new PaymentResponse().status(PaymentResponse.StatusEnum.FAILED)));

        StepVerifier.create(buyService.buy(cartId))
                .expectError(PaymentException.InsufficientFunds.class)
                .verify();

        verify(orderRepository, never()).save(any());
    }
}
