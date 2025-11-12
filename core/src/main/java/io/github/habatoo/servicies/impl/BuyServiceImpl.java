package io.github.habatoo.servicies.impl;

import io.github.habatoo.entity.Cart;
import io.github.habatoo.entity.CartItem;
import io.github.habatoo.entity.Order;
import io.github.habatoo.entity.OrderItem;
import io.github.habatoo.repositories.CartItemRepository;
import io.github.habatoo.repositories.CartRepository;
import io.github.habatoo.repositories.OrderItemRepository;
import io.github.habatoo.repositories.OrderRepository;
import io.github.habatoo.servicies.BuyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Реализация для осуществления покупки.
 * Предоставляет бизнес-логику для совершения покупки.
 */
@Slf4j
@Service
public class BuyServiceImpl implements BuyService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    public BuyServiceImpl(OrderRepository orderRepository,
                          OrderItemRepository orderItemRepository,
                          CartRepository cartRepository,
                          CartItemRepository cartItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public void buy(Long cartId) {
        log.debug("Получение корзины id={}", cartId);
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalStateException("Корзина с id=%d не найдена".formatted(cartId)));
        log.info("Корзина успешно получена: id={}, itemsCount={}", cartId, cart.getItems().size());

        Order order = new Order();
        order.setDateTime(LocalDateTime.now());
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalSum = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            int count = cartItem.getCount();
            BigDecimal price = cartItem.getPrice();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setItem(cartItem.getItem());
            orderItem.setCount(count);
            orderItem.setPrice(price);

            BigDecimal itemSum = price.multiply(BigDecimal.valueOf(count));
            totalSum = totalSum.add(itemSum);
            orderItems.add(orderItem);

            log.debug("Создан заказанный товар: itemId={}, count={}, itemSum={}", cartItem.getItem().getId(), count, itemSum);
        }
        order.setItems(orderItems);
        order.setTotalSum(totalSum);

        log.info("Итоговая сумма заказа: {}", totalSum);

        orderRepository.save(order);
        log.info("Заказ сохранен: orderId={}, itemsCount={}", order.getId(), orderItems.size());

        orderItems.forEach(orderItem -> {
            orderItemRepository.save(orderItem);
            log.debug("Товар заказа сохранен: itemId={}, orderId={}", orderItem.getItem().getId(), order.getId());
        });

        cartItemRepository.deleteAll(cart.getItems());
        log.debug("Товары корзины удалены: cartId={}, count={}", cartId, orderItems.size());
        cart.getItems().clear();
        cart.setTotal(BigDecimal.ZERO);
        cartRepository.save(cart);
        log.info("Корзина очищена и сохранена: cartId={}", cartId);
    }
}
