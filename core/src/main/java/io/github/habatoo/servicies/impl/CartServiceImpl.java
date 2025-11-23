package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.enums.Action;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.dto.response.CartItemDto;
import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.entity.Cart;
import io.github.habatoo.entity.CartItem;
import io.github.habatoo.mappers.CartMapper;
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
import java.util.NoSuchElementException;

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
    private final CartMapper cartMapper;
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
                                .filter(ci -> ci.getItemId().equals(itemId))
                                .next()
                                .flatMap(cartItem -> {
                                    int newCount = cartItem.getCount() + (request.getAction() == Action.PLUS ? 1 : -1);
                                    if (newCount > 0) {
                                        cartItem.setCount(newCount);
                                        return cartItemRepository.save(cartItem)
                                                .flatMap(savedCartItem -> itemRepository.findById(
                                                        savedCartItem.getItemId()));
                                    } else {
                                        return cartItemRepository.delete(cartItem).then(Mono.empty());
                                    }
                                })
                                .switchIfEmpty(
                                        request.getAction() == Action.PLUS
                                                ? itemRepository.findById(itemId)
                                                .flatMap(item -> {
                                                    CartItem newCartItem = new CartItem();
                                                    newCartItem.setCartId(cart.getId());
                                                    newCartItem.setItemId(item.getId());
                                                    newCartItem.setCount(1);
                                                    newCartItem.setPrice(item.getPrice());
                                                    return cartItemRepository.save(newCartItem)
                                                            .thenReturn(item);
                                                })
                                                : Mono.empty()
                                )
                                .flatMap(item -> recalculateCartTotal(cart.getId()).thenReturn(item))
                                .map(item -> {
                                    return new ItemDto(
                                            item.getId(),
                                            item.getTitle(),
                                            item.getDescription(),
                                            item.getImgPath(),
                                            item.getPrice(),
                                            0
                                    );
                                })
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
                                .flatMap(cartItem ->
                                        itemRepository.findById(cartItem.getItemId())
                                                .map(itemMapper::toDto)
                                                .map(itemDto -> CartItemDto.builder()
                                                        .item(itemDto)
                                                        .count(cartItem.getCount())
                                                        .price(cartItem.getPrice())
                                                        .build()
                                                )
                                )
                                .collectList()
                                .map(itemsDto -> {
                                    BigDecimal total = itemsDto.stream()
                                            .map(i -> i.price().multiply(BigDecimal.valueOf(i.count())))
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                                    CartDto cartDto = cartMapper.toDto(cart);

                                    return CartDto.builder()
                                            .id(cartDto.id())
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
        log.info("Запрошено изменение и получение корзины: request={}", request);

        return changeNumberOfItems(request)
                .doOnNext(item -> log.debug("Изменение товара выполнено: itemId={}", item.id()))
                .switchIfEmpty(Mono.error(new NoSuchElementException("Товар не найден для изменения")))
                .then(getItemsInTheCart())
                .doOnNext(cart -> log.debug("Возврат DTO корзины: {}", cart));
    }

    public Mono<Void> recalculateCartTotal(Long cartId) {
        return cartItemRepository.findAllByCartId(cartId)
                .flatMap(cartItem -> itemRepository.findById(cartItem.getItemId())
                        .map(item -> cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getCount())))
                )
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .flatMap(total -> cartRepository.findById(cartId)
                        .flatMap(cart -> {
                            cart.setTotal(total);
                            return cartRepository.save(cart);
                        })
                ).then();
    }

    private Mono<Cart> getCurrentCart() {
        return cartRepository.findAll()
                .take(1)
                .singleOrEmpty()
                .doOnNext(cart -> log.debug("Используется корзина: cartId={}", cart.getId()))
                .switchIfEmpty(
                        Mono.defer(() -> {
                            log.info("Корзина не найдена. Создаём новую корзину.");
                            return cartRepository.save(new Cart());
                        })
                );
    }
}
