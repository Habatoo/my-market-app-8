package io.github.habatoo.servicies.impl;

import io.github.habatoo.entity.CartItem;
import io.github.habatoo.entity.Item;
import io.github.habatoo.entity.Order;
import io.github.habatoo.entity.OrderItem;
import io.github.habatoo.repositories.*;
import io.github.habatoo.servicies.BuyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

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
    private final ItemRepository itemRepository;

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public Mono<Void> buy(Long cartId) {

        log.debug("Получение корзины id={}", cartId);

        return cartRepository.findById(cartId)
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "Корзина с id=%d не найдена".formatted(cartId)
                )))
                .flatMap(cart ->

                        cartItemRepository.findAllByCartId(cart.getId())
                                .flatMap(cartItem ->
                                        itemRepository.findById(cartItem.getItemId())
                                                .switchIfEmpty(Mono.error(new IllegalStateException(
                                                        "Товар id=%d не найден".formatted(cartItem.getItemId())
                                                )))
                                                .map(item -> Tuples.of(
                                                        cartItem,
                                                        item,
                                                        cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getCount()))
                                                ))
                                )
                                .collectList()
                                .flatMap(tuples -> {

                                    Order order = new Order();
                                    order.setDateTime(LocalDateTime.now());
                                    order.setTotalSum(
                                            tuples.stream()
                                                    .map(Tuple3::getT3)
                                                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                                    );

                                    log.info("Итоговая сумма заказа: {}", order.getTotalSum());

                                    return orderRepository.save(order)
                                            .flatMap(savedOrder -> {

                                                log.info("Заказ сохранён: orderId={}", savedOrder.getId());

                                                return Flux.fromIterable(tuples)
                                                        .flatMap(tuple -> {

                                                            CartItem cartItem = tuple.getT1();
                                                            Item item = tuple.getT2();
                                                            BigDecimal itemSum = tuple.getT3();

                                                            OrderItem oi = new OrderItem();
                                                            oi.setOrderId(savedOrder.getId());
                                                            oi.setItemId(item.getId());
                                                            oi.setCount(cartItem.getCount());
                                                            oi.setPrice(cartItem.getPrice());

                                                            log.debug(
                                                                    "Создан заказанный товар: itemId={}, count={}, itemSum={}",
                                                                    item.getId(), cartItem.getCount(), itemSum
                                                            );

                                                            return orderItemRepository.save(oi);
                                                        })
                                                        .then(
                                                                cartItemRepository.deleteAllByCartId(cart.getId())
                                                        )
                                                        .then(
                                                                Mono.defer(() -> {
                                                                    cart.setTotal(BigDecimal.ZERO);
                                                                    return cartRepository.save(cart);
                                                                })
                                                        );
                                            });
                                })

                ).then();
    }
}
