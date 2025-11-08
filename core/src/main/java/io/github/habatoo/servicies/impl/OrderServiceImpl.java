package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.dto.response.OrderItemDto;
import io.github.habatoo.entity.*;
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

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderItemRepository orderItemRepository,
                            CartRepository cartRepository,
                            CartItemRepository cartItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
    }

    /**
     * Получить список заказов с корректным расчетом сумм DTO
     */
    @Transactional(readOnly = true)
    @Override
    public List<OrderDto> getOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Получить конкретный заказ по id
     */
    @Transactional(readOnly = true)
    @Override
    public OrderDto getOrder(Long id, boolean newOrder) {
        Order order = orderRepository.findById(id).orElse(null);
        return order != null ? toDto(order) : null;
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

        // Сохраняем заказ с позициями
        orderRepository.save(order);
        orderItems.forEach(orderItemRepository::save);

        // Очищаем корзину пользователя
        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();
        cart.setTotal(BigDecimal.ZERO);
        cartRepository.save(cart);
    }

    /**
     * Маппинг сущности Order в DTO с гарантированным расчетом total
     */
    public OrderDto toDto(Order order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(this::toOrderItemDto)
                .collect(Collectors.toList());
        return new OrderDto(
                order.getId(),
                itemDtos,
                order.getTotalSum(),
                order.getDateTime()
        );
    }

    /**
     * Маппинг позиции заказа с расчетом суммы по позиции (total)
     */
    public OrderItemDto toOrderItemDto(OrderItem entity) {
        BigDecimal total = entity.getPrice().multiply(BigDecimal.valueOf(entity.getCount()));
        return new OrderItemDto(
                toItemDto(entity.getItem()),
                null, // OrderDto не требуется для фронта
                entity.getCount(),
                entity.getPrice(),
                total
        );
    }

    /**
     * Маппинг товара из OrderItem (обычно простой)
     */
    private ItemDto toItemDto(Item item) {
        return new ItemDto(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getImgPath(),
                item.getPrice(),
                0 // здесь count не нужен!
        );
    }
}
