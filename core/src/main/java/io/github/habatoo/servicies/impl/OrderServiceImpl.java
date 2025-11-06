package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.entity.Cart;
import io.github.habatoo.entity.Order;
import io.github.habatoo.mappers.BaseMapper;
import io.github.habatoo.repositories.CartRepository;
import io.github.habatoo.repositories.OrderRepository;
import io.github.habatoo.servicies.AbstractService;
import io.github.habatoo.servicies.OrderService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Реализация для работы с заказами.
 * Предоставляет бизнес-логику для операций с отображением заказов и совершением покупки.
 */
public class OrderServiceImpl extends AbstractService<Order, OrderDto> implements OrderService {

    private final CartRepository cartRepository;
    private final BaseMapper<Cart, CartDto> cartMapper;

    public OrderServiceImpl(
            OrderRepository repository,
            BaseMapper<Order, OrderDto> mapper,
            CartRepository cartRepository,
            BaseMapper<Cart, CartDto> cartMapper
    ) {
        super(repository, mapper);
        this.cartRepository = cartRepository;
        this.cartMapper = cartMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<OrderDto> getOrders() {
        List<Order> entities = repository.findAll();
        return entities.stream()
                .map(mapper::toDto)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OrderDto getOrder(Long id, boolean newOrder) {
        return getById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buy(Long id) {
        CartDto cart = cartRepository.findById(id)
                .map(cartMapper::toDto)
                .orElse(null);
        Order order = new Order();
        order.setItems(List.of()); //TODO
        order.setTotalSum(cart.total());
        order.setDateTime(LocalDateTime.now());
        repository.save(order);
    }
}
