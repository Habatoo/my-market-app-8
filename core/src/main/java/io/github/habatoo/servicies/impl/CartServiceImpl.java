package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.servicies.CartService;

/**
 * Реализация для работы с корзиной.
 * Предоставляет бизнес-логику для операций с товарами в корзине.
 */
public class CartServiceImpl implements CartService {

    /**
     * {@inheritDoc}
     */
    @Override
    public void changeNumberOfItems(ChangeNumberOfItemsRequestDto request) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemDto changeNumberOfItemsFromPage(ChangeNumberOfItemsRequestDto request) {
        return null;
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
        return null;
    }
}
