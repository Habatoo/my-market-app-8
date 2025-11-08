package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.enums.Action;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.entity.Cart;
import io.github.habatoo.entity.CartItem;
import io.github.habatoo.entity.Item;
import io.github.habatoo.mappers.CartMapper;
import io.github.habatoo.repositories.CartItemRepository;
import io.github.habatoo.repositories.CartRepository;
import io.github.habatoo.repositories.ItemRepository;
import io.github.habatoo.servicies.CartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Реализация для работы с корзиной.
 * Предоставляет бизнес-логику для операций с товарами в корзине.
 */
@Service
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final CartMapper cartMapper;

    public CartServiceImpl(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ItemRepository itemRepository,
            CartMapper cartMapper) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.itemRepository = itemRepository;
        this.cartMapper = cartMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public void changeNumberOfItems(ChangeNumberOfItemsRequestDto request) {
        Cart cart = getCurrentCart();
        Item item = itemRepository.findById(request.getId()).orElse(null);
        if (item == null) return;

        CartItem cartItem = cart.getItems().stream()
                .filter(ci -> ci.getItem().getId().equals(item.getId()))
                .findFirst().orElse(null);

        if (cartItem == null && request.getAction() == Action.PLUS) {
            cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setItem(item);
            cartItem.setCount(1);
            cartItem.setPrice(item.getPrice());
            cart.getItems().add(cartItem);
            cartItemRepository.save(cartItem);
        } else if (cartItem != null) {
            int newCount = cartItem.getCount() + (request.getAction() == Action.PLUS ? 1 : -1);
            if (newCount > 0) {
                cartItem.setCount(newCount);
                cartItemRepository.save(cartItem);
            } else {
                cart.getItems().remove(cartItem);
                cartItemRepository.delete(cartItem);
            }
        }

        recalculateCartTotal(cart);
        cartRepository.save(cart);
    }

    /**
     * {@inheritDoc}
     */
    @Transactional(readOnly = true)
    @Override
    public CartDto getItemsInTheCart() {
        Cart cart = getCurrentCart();
        return cartMapper.toDto(cart);
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public CartDto changeNumberOfItemsFromCart(ChangeNumberOfItemsRequestDto request) {
        changeNumberOfItems(request);
        return getItemsInTheCart();
    }

    private void recalculateCartTotal(Cart cart) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem ci : cart.getItems()) {
            total = total.add(ci.getPrice().multiply(BigDecimal.valueOf(ci.getCount())));
        }
        cart.setTotal(total);
    }

    private Cart getCurrentCart() {
        return cartRepository.findAll().stream().findFirst()
                .orElseGet(() -> cartRepository.save(new Cart()));
    }
}
