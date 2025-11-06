package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.Action;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.entity.Cart;
import io.github.habatoo.mappers.BaseMapper;
import io.github.habatoo.repositories.CartItemRepository;
import io.github.habatoo.repositories.CartRepository;
import io.github.habatoo.servicies.AbstractService;
import io.github.habatoo.servicies.CartService;
import org.springframework.stereotype.Service;

/**
 * Реализация для работы с корзиной.
 * Предоставляет бизнес-логику для операций с товарами в корзине.
 */
@Service
public class CartServiceImpl extends AbstractService<Cart, CartDto> implements CartService {

    private final CartItemRepository cartItemMepository;

    public CartServiceImpl(
            CartRepository repository,
            BaseMapper<Cart, CartDto> mapper,
            CartItemRepository cartItemMepository
    ) {
        super(repository, mapper);
        this.cartItemMepository = cartItemMepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changeNumberOfItems(ChangeNumberOfItemsRequestDto request) {
        if (Action.MINUS.equals(request.getAction())) {
            var cartItem = cartItemMepository.findById(request.getId()).orElse(null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CartDto getItemsInTheCart() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CartDto changeNumberOfItemsFromCart(ChangeNumberOfItemsRequestDto request) {
        return getById(request.getId());
    }
}
