package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.enums.Action;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.entity.Cart;
import io.github.habatoo.entity.CartItem;
import io.github.habatoo.entity.Item;
import io.github.habatoo.mappers.CartMapper;
import io.github.habatoo.mappers.ItemMapper;
import io.github.habatoo.repositories.CartItemRepository;
import io.github.habatoo.repositories.CartRepository;
import io.github.habatoo.repositories.ItemRepository;
import io.github.habatoo.servicies.CartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Реализация для работы с корзиной.
 * Предоставляет бизнес-логику для операций с товарами в корзине.
 */
@Slf4j
@Service
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final CartMapper cartMapper;
    private final ItemMapper itemMapper;

    public CartServiceImpl(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ItemRepository itemRepository,
            CartMapper cartMapper,
            ItemMapper itemMapper) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.itemRepository = itemRepository;
        this.cartMapper = cartMapper;
        this.itemMapper = itemMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public ItemDto changeNumberOfItems(ChangeNumberOfItemsRequestDto request) {
        log.debug("Запрошено изменение позиций корзины: request={}", request);
        Cart cart = getCurrentCart();
        Long itemId = request.getId();
        log.debug("Получение товара с id={}", itemId);
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalStateException("Товар с id=%d не найден".formatted(itemId)));

        CartItem cartItem = cart.getItems().stream()
                .filter(ci -> ci.getItem().getId().equals(item.getId()))
                .findFirst().orElse(null);

        log.debug("Текущий CartItem для товара с id={} найден: {}", itemId, cartItem);

        if (cartItem == null && request.getAction() == Action.PLUS) {
            log.info("Добавление нового товара в корзину: itemId={}", itemId);
            cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setItem(item);
            cartItem.setCount(1);
            cartItem.setPrice(item.getPrice());
            cart.getItems().add(cartItem);
            cartItemRepository.save(cartItem);
        } else if (cartItem != null) {
            int newCount = cartItem.getCount() + (request.getAction() == Action.PLUS ? 1 : -1);
            log.debug("Обновление количества товара itemId={}, старое={}, новое={}", itemId, cartItem.getCount(), newCount);
            if (newCount > 0) {
                cartItem.setCount(newCount);
                cartItemRepository.save(cartItem);
                log.info("Количество товара обновлено: itemId={}, count={}", itemId, newCount);
            } else {
                cart.getItems().remove(cartItem);
                cartItemRepository.delete(cartItem);
                log.info("Товар удалён из корзины: itemId={}", itemId);
            }
        }

        recalculateCartTotal(cart);
        log.debug("После пересчёта стоимость корзины: cartId={}, total={}", cart.getId(), cart.getTotal());
        cartRepository.save(cart);

        ItemDto result = itemMapper.toDto(item);
        log.debug("Возврат DTO товара: {}", result);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public CartDto getItemsInTheCart() {
        Cart cart = getCurrentCart();
        log.info("Получение содержимого корзины: cartId={}, itemsCount={}", cart.getId(), cart.getItems().size());
        return cartMapper.toDto(cart);
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public CartDto changeNumberOfItemsFromCart(ChangeNumberOfItemsRequestDto request) {
        log.info("Запрошено изменение и получение корзины: request={}", request);
        changeNumberOfItems(request);
        CartDto result = getItemsInTheCart();
        log.debug("Возврат DTO корзины: {}", result);
        return result;
    }

    private void recalculateCartTotal(Cart cart) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem ci : cart.getItems()) {
            total = total.add(ci.getPrice().multiply(BigDecimal.valueOf(ci.getCount())));
            log.trace("Считаем позицию корзины: itemId={}, price={}, count={}", ci.getItem().getId(), ci.getPrice(), ci.getCount());
        }
        cart.setTotal(total);
        log.debug("Итого стоимость корзины: {}", total);
    }

    private Cart getCurrentCart() {
        Cart cart = cartRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    log.info("Корзина не найдена. Создаём новую корзину.");
                    return cartRepository.save(new Cart());
                });
        log.debug("Используется корзина: cartId={}", cart.getId());
        return cart;
    }
}
