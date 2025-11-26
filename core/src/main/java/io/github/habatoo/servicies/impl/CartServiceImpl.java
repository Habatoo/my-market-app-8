package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.enums.Action;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.dto.response.CartItemDto;
import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.entity.Cart;
import io.github.habatoo.entity.CartItem;
import io.github.habatoo.mappers.ItemMapper;
import io.github.habatoo.repositories.CartItemRepository;
import io.github.habatoo.repositories.CartRepository;
import io.github.habatoo.repositories.ItemRepository;
import io.github.habatoo.servicies.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Реализация для работы с корзиной.
 * Предоставляет бизнес-логику для операций с товарами в корзине.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public Mono<ItemDto> changeNumberOfItems(ChangeNumberOfItemsRequestDto request) {
        Long itemId = request.getId();

        return getCurrentCart()
                .flatMap(cart ->
                        cartItemRepository.findAllByCartId(cart.getId())
                                .filter(ci -> Objects.equals(ci.getItemId(), itemId))
                                .next()
                                .flatMap(existingCi -> {
                                    int newCount = existingCi.getCount() + (request.getAction() == Action.PLUS ? 1 : -1);
                                    if (newCount > 0) {
                                        existingCi.setCount(newCount);
                                        return cartItemRepository.save(existingCi)
                                                .flatMap(saved -> itemRepository.findById(saved.getItemId()))
                                                .map(itemMapper::toDto);
                                    } else {
                                        return cartItemRepository.delete(existingCi)
                                                .then(recalcAndSaveCartTotal(cart.getId()))
                                                .then(itemRepository.findById(existingCi.getItemId())
                                                        .map(itemMapper::toDto)
                                                        .defaultIfEmpty(toDeletedItemDto(existingCi.getItemId()))
                                                );
                                    }
                                })
                                .switchIfEmpty(
                                        request.getAction() == Action.PLUS
                                                ? itemRepository.findById(itemId)
                                                .flatMap(item -> {
                                                    CartItem newCi = new CartItem();
                                                    newCi.setCartId(cart.getId());
                                                    newCi.setItemId(item.getId());
                                                    newCi.setCount(1);
                                                    newCi.setPrice(item.getPrice());
                                                    return cartItemRepository.save(newCi)
                                                            .then(recalcAndSaveCartTotal(cart.getId()))
                                                            .thenReturn(itemMapper.toDto(item));
                                                })
                                                : Mono.empty()
                                )
                );
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public Mono<CartDto> getItemsInTheCart() {
        return getCurrentCart()
                .flatMap(cart ->
                        cartItemRepository.findAllByCartId(cart.getId())
                                .flatMap(ci ->
                                        itemRepository.findById(ci.getItemId())
                                                .map(itemMapper::toDto)
                                                .map(itemDto -> CartItemDto.builder()
                                                        .item(itemDto)
                                                        .count(ci.getCount())
                                                        .price(ci.getPrice())
                                                        .build()
                                                )
                                )
                                .collectList()
                                .map(itemsDto -> {
                                    BigDecimal total = itemsDto.stream()
                                            .map(i -> i.price().multiply(BigDecimal.valueOf(i.count())))
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                                    return CartDto.builder()
                                            .id(cart.getId())
                                            .items(itemsDto)
                                            .total(total)
                                            .build();
                                })
                );
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public Mono<CartDto> changeNumberOfItemsFromCart(ChangeNumberOfItemsRequestDto request) {
        return changeNumberOfItems(request)
                .then(getItemsInTheCart());
    }

    private Mono<Cart> getCurrentCart() {
        return cartRepository.findAll()
                .next()
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Корзина не найдена. Создаём новую корзину.");
                    return cartRepository.save(new Cart());
                }))
                .doOnNext(c -> log.debug("Используется корзина: cartId={}", c.getId()));
    }

    private Mono<Void> recalcAndSaveCartTotal(Long cartId) {
        return cartItemRepository.findAllByCartId(cartId)
                .map(ci -> ci.getPrice().multiply(BigDecimal.valueOf(ci.getCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .flatMap(total -> cartRepository.findById(cartId)
                        .flatMap(cart -> {
                            cart.setTotal(total);
                            return cartRepository.save(cart);
                        })
                )
                .then();
    }

    private ItemDto toDeletedItemDto(Long itemId) {
        return new ItemDto(itemId, null, null, null, null, 0);
    }
}
