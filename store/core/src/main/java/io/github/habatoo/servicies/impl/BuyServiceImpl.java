package io.github.habatoo.servicies.impl;

import io.github.habatoo.entity.Order;
import io.github.habatoo.entity.OrderItem;
import io.github.habatoo.repositories.CartItemRepository;
import io.github.habatoo.repositories.CartRepository;
import io.github.habatoo.repositories.OrderItemRepository;
import io.github.habatoo.repositories.OrderRepository;
import io.github.habatoo.servicies.BuyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @Transactional
    @Override
    public Mono<Long> buy(Long cartId) {
        return cartRepository.findById(cartId)
                .switchIfEmpty(Mono.error(new IllegalStateException("Корзина с id=" + cartId + " не найдена")))
                .flatMap(cart ->
                        cartItemRepository.findAllByCartId(cart.getId()).collectList()
                                .flatMap(cartItems -> {
                                    if (cartItems.isEmpty()) {
                                        return Mono.error(new IllegalStateException("В корзине нет товаров для покупки"));
                                    }

                                    Order order = new Order();
                                    order.setDateTime(LocalDateTime.now());
                                    order.setTotalSum(cartItems.stream()
                                            .map(ci -> ci.getPrice().multiply(
                                                    BigDecimal.valueOf(ci.getCount())))
                                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                                    );

                                    return orderRepository.save(order)
                                            .flatMap(savedOrder ->
                                                    Flux.fromIterable(cartItems)
                                                            .flatMap(ci -> {
                                                                OrderItem oi = new OrderItem();
                                                                oi.setOrderId(savedOrder.getId());
                                                                oi.setItemId(ci.getItemId());
                                                                oi.setCount(ci.getCount());
                                                                oi.setPrice(ci.getPrice());
                                                                return orderItemRepository.save(oi);
                                                            })
                                                            .collectList()
                                                            .then(cartItemRepository.deleteAllByCartId(cart.getId()))
                                                            .then(cartRepository.findById(cart.getId())
                                                                    .flatMap(c -> {
                                                                        c.setTotal(BigDecimal.ZERO);
                                                                        return cartRepository.save(c);
                                                                    })
                                                            )
                                                            .thenReturn(savedOrder.getId())
                                            );
                                })
                );
    }
}
