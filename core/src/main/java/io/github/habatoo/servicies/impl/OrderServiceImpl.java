package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.entity.Cart;
import io.github.habatoo.entity.CartItem;
import io.github.habatoo.entity.Order;
import io.github.habatoo.entity.OrderItem;
import io.github.habatoo.mappers.OrderMapper;
import io.github.habatoo.repositories.CartItemRepository;
import io.github.habatoo.repositories.CartRepository;
import io.github.habatoo.repositories.OrderItemRepository;
import io.github.habatoo.repositories.OrderRepository;
import io.github.habatoo.servicies.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Реализация для работы с заказами.
 * Предоставляет бизнес-логику для операций с отображением заказов и совершением покупки.
 */
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderMapper mapper;

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderItemRepository orderItemRepository,
                            CartRepository cartRepository,
                            CartItemRepository cartItemRepository,
                            OrderMapper mapper) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.mapper = mapper;
    }

    /**
     * Получить список заказов с корректным расчетом сумм DTO
     */
    @Transactional(readOnly = true)
    @Override
    public List<OrderDto> getOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Получить конкретный заказ по id
     */
    @Transactional(readOnly = true)
    @Override
    public OrderDto getOrder(Long id, boolean newOrder) {
        Order order = orderRepository.findById(id).orElse(null);
        return order != null ? mapper.toDto(order) : null;
    }

    /**
     * Совершить покупку из корзины: сохранить заказ, рассчитать все суммы
     */
    @Transactional
    @Override
    public void buy(Long cartId) {
        Cart cart = cartRepository.findById(cartId).orElse(null);
        if (cart == null || cart.getItems().isEmpty()) return;

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
        }
        order.setItems(orderItems);
        order.setTotalSum(totalSum);

        orderRepository.save(order);
        orderItems.forEach(orderItemRepository::save);

        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();
        cart.setTotal(BigDecimal.ZERO);
        cartRepository.save(cart);
    }
}
