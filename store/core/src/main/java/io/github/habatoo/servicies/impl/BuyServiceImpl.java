package io.github.habatoo.servicies.impl;

import io.github.habatoo.entity.Cart;
import io.github.habatoo.entity.CartItem;
import io.github.habatoo.entity.Order;
import io.github.habatoo.entity.OrderItem;
import io.github.habatoo.exceptions.InsufficientFundsException;
import io.github.habatoo.exceptions.PaymentServiceUnavailableException;
import io.github.habatoo.repositories.CartItemRepository;
import io.github.habatoo.repositories.CartRepository;
import io.github.habatoo.repositories.OrderItemRepository;
import io.github.habatoo.repositories.OrderRepository;
import io.github.habatoo.servicies.BuyService;
import io.github.habatoo.store.payment.api.PaymentsApi;
import io.github.habatoo.store.payment.model.PaymentRequest;
import io.github.habatoo.store.payment.model.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Реализация для осуществления покупки.
 * Предоставляет бизнес-логику для совершения покупки.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuyServiceImpl implements BuyService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final PaymentsApi paymentsApi;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Long> buy(Long cartId) {
        return findCartOrError(cartId)
                .flatMap(cart ->
                        loadItemsOrError(cart.getId())
                                .flatMap(items -> processPurchase(cart, items))
                );
    }

    private Mono<Cart> findCartOrError(Long id) {
        return cartRepository.findById(id)
                .switchIfEmpty(Mono.error(
                        new IllegalStateException("Корзина с id=" + id + " не найдена")));
    }

    private Mono<List<CartItem>> loadItemsOrError(Long cartId) {
        return cartItemRepository.findAllByCartId(cartId)
                .collectList()
                .flatMap(items -> {
                    if (items.isEmpty()) {
                        return Mono.error(
                                new IllegalStateException("В корзине нет товаров для покупки"));
                    }
                    return Mono.just(items);
                });
    }

    private Mono<Long> processPurchase(Cart cart, List<CartItem> items) {
        BigDecimal totalAmount = calculateTotalAmount(items);

        return processPayment(totalAmount)
                .then(placeOrder(items, totalAmount))
                .flatMap(orderId -> clearCart(cart.getId()).thenReturn(orderId));
    }

    private BigDecimal calculateTotalAmount(List<CartItem> cartItems) {
        return cartItems.stream()
                .map(ci -> ci.getPrice().multiply(BigDecimal.valueOf(ci.getCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Mono<Void> processPayment(BigDecimal totalAmount) {
        PaymentRequest request = new PaymentRequest().amount(totalAmount);

        return paymentsApi.createPayment("application/json", request)
                .flatMap(response -> {
                    if (response.getStatus() == PaymentResponse.StatusEnum.SUCCESS) {
                        return Mono.empty();
                    }
                    return Mono.error(new InsufficientFundsException());
                })
                .onErrorMap(ex -> {
                    if (ex instanceof InsufficientFundsException) {
                        return ex;
                    }
                    log.error("Ошибка сервиса платежей", ex);

                    return new PaymentServiceUnavailableException();
                }).then();
    }

    private Mono<Long> placeOrder(List<CartItem> cartItems, BigDecimal totalAmount) {
        return saveOrder(createOrderEntity(totalAmount))
                .flatMap(order ->
                        saveOrderItems(order.getId(), cartItems)
                                .thenReturn(order.getId())
                );
    }

    private Mono<Order> saveOrder(Order order) {
        return orderRepository.save(order);
    }

    private Order createOrderEntity(BigDecimal totalAmount) {
        Order order = new Order();
        order.setDateTime(LocalDateTime.now());
        order.setTotalSum(totalAmount);

        return order;
    }

    private Mono<Void> saveOrderItems(Long orderId, List<CartItem> cartItems) {
        return Flux.fromIterable(cartItems)
                .map(ci -> {
                    OrderItem oi = new OrderItem();
                    oi.setOrderId(orderId);
                    oi.setItemId(ci.getItemId());
                    oi.setCount(ci.getCount());
                    oi.setPrice(ci.getPrice());
                    return oi;
                })
                .flatMap(orderItemRepository::save)
                .then();
    }

    private Mono<Void> clearCart(Long cartId) {
        return deleteCartItems(cartId)
                .then(resetCartTotal(cartId));
    }

    private Mono<Void> deleteCartItems(Long cartId) {
        return cartItemRepository.deleteAllByCartId(cartId);
    }

    private Mono<Void> resetCartTotal(Long cartId) {
        return cartRepository.findById(cartId)
                .flatMap(cart -> {
                    cart.setTotal(BigDecimal.ZERO);
                    return cartRepository.save(cart);
                })
                .then();
    }
}
