package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.enums.Action;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.dto.response.CartItemDto;
import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.entity.Cart;
import io.github.habatoo.entity.CartItem;
import io.github.habatoo.entity.Item;
import io.github.habatoo.entity.User;
import io.github.habatoo.mappers.ItemMapper;
import io.github.habatoo.repositories.CartItemRepository;
import io.github.habatoo.repositories.CartRepository;
import io.github.habatoo.repositories.ItemRepository;
import io.github.habatoo.repositories.UserRepository;
import io.github.habatoo.servicies.CartService;
import io.github.habatoo.store.payment.api.PaymentsApi;
import io.github.habatoo.store.payment.model.BalanceResponse;
import io.github.habatoo.store.payment.model.PaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
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
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final PaymentsApi paymentsApi;

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public Mono<ItemDto> changeNumberOfItems(ChangeNumberOfItemsRequestDto request) {
        return getCurrentCart()
                .flatMap(cart -> findCartItem(cart.getId(), request.getId())
                        .flatMap(existingCi -> processExistingItem(cart, existingCi, request.getAction()))
                        .switchIfEmpty(handleNewItem(cart, request)));
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public Mono<CartDto> getItemsInTheCart() {
        return getCurrentCart()
                .flatMap(this::assembleCartDto);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Boolean> canProcessPayment(PaymentRequest request) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    return paymentsApi.getWalletBalance()
                            .map(BalanceResponse::getBalance)
                            .map(balance -> balance.compareTo(request.getAmount()) >= 0)
                            .doOnError(e -> log.error("ОШИБКА ВЫЗОВА PAYMENT-SERVICE: {}", e.getMessage()))
                            .onErrorReturn(false);
                });
    }

    private Mono<Cart> getCurrentCart() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    if (!isUserAuthenticated(auth)) {
                        log.warn("Пользователь не аутентифицирован");
                        return Mono.empty();
                    }

                    String externalId = extractExternalId(auth);
                    if (externalId == null) {
                        log.warn("Не удалось извлечь externalId из токена типа: {}", auth.getClass().getName());
                        return Mono.empty();
                    }

                    return userRepository.findByExternalId(externalId)
                            .switchIfEmpty(Mono.defer(() -> syncUserWithDatabase(auth)))
                            .flatMap(user -> cartRepository.findByUserId(user.getId())
                                    .doOnNext(c -> log.debug("Корзина найдена для пользователя: {}", user.getUsername()))
                                    .switchIfEmpty(Mono.defer(() -> createNewCart(user))));
                });
    }

    private String extractExternalId(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getSubject();
        } else if (auth instanceof OAuth2AuthenticationToken oauth) {
            return oauth.getPrincipal().getAttribute("sub");
        } else if (auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }

        return null;
    }

    private Mono<User> syncUserWithDatabase(Authentication auth) {
        String externalId = extractExternalId(auth);
        String username = getUsername(auth);

        log.info("Синхронизация нового пользователя из Keycloak: username={}, externalId={}", username, externalId);

        User newUser = getNewUser(externalId, username);

        return userRepository.save(newUser)
                .doOnError(e -> log.error("Ошибка при сохранении пользователя в БД: {}", e.getMessage()));
    }

    private User getNewUser(String externalId, String username) {
        User newUser = new User();
        newUser.setExternalId(externalId);
        newUser.setUsername(username != null ? username : "user_" + externalId.substring(0, 8));
        newUser.setRole("USER");

        return newUser;
    }

    private String getUsername(Authentication auth) {
        String username = null;

        if (auth instanceof OAuth2AuthenticationToken oauth && oauth.getPrincipal() instanceof OidcUser oidcUser) {
            username = oidcUser.getPreferredUsername();
        } else if (auth instanceof JwtAuthenticationToken jwtAuth) {
            username = jwtAuth.getToken().getClaimAsString("preferred_username");
        }

        return username;
    }

    private Mono<Cart> createNewCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Создание НОВОЙ корзины для пользователя: {}", user.getUsername());

                    Cart newCart = new Cart();
                    newCart.setUserId(user.getId());
                    newCart.setTotal(BigDecimal.ZERO);

                    return cartRepository.save(newCart);
                }));
    }

    private Mono<CartItem> findCartItem(Long cartId, Long itemId) {
        return cartItemRepository.findAllByCartId(cartId)
                .filter(ci -> Objects.equals(ci.getItemId(), itemId))
                .next();
    }

    private Mono<ItemDto> processExistingItem(Cart cart, CartItem ci, Action action) {
        int newCount = ci.getCount() + (action == Action.PLUS ? 1 : -1);
        return newCount > 0 ? updateItemCount(ci, newCount) : removeItem(cart, ci);
    }

    private Mono<ItemDto> updateItemCount(CartItem ci, int newCount) {
        ci.setCount(newCount);
        return cartItemRepository.save(ci)
                .flatMap(saved -> itemRepository.findById(saved.getItemId()))
                .map(itemMapper::toDto);
    }

    private Mono<ItemDto> removeItem(Cart cart, CartItem ci) {
        return cartItemRepository.delete(ci)
                .then(recalcAndSaveCartTotal(cart.getId()))
                .then(itemRepository.findById(ci.getItemId())
                        .map(itemMapper::toDto)
                        .defaultIfEmpty(toDeletedItemDto(ci.getItemId())));
    }

    private Mono<ItemDto> handleNewItem(Cart cart, ChangeNumberOfItemsRequestDto request) {
        if (request.getAction() != Action.PLUS) return Mono.empty();
        return itemRepository.findById(request.getId())
                .flatMap(item -> createCartItem(cart.getId(), item))
                .flatMap(ci -> recalcAndSaveCartTotal(cart.getId()).thenReturn(ci))
                .flatMap(ci -> itemRepository.findById(ci.getItemId()))
                .map(itemMapper::toDto);
    }

    private Mono<CartItem> createCartItem(Long cartId, Item item) {
        CartItem cartItem = new CartItem();
        cartItem.setCartId(cartId);
        cartItem.setItemId(item.getId());
        cartItem.setCount(1);
        cartItem.setPrice(item.getPrice());

        return cartItemRepository.save(cartItem);
    }

    private Mono<CartDto> assembleCartDto(Cart cart) {
        return cartItemRepository.findAllByCartId(cart.getId())
                .flatMap(this::toCartItemDto)
                .collectList()
                .map(items -> CartDto.builder()
                        .id(cart.getId())
                        .items(items)
                        .total(calculateTotal(items))
                        .build());
    }

    private Mono<CartItemDto> toCartItemDto(CartItem ci) {
        return itemRepository.findById(ci.getItemId())
                .map(itemMapper::toDto)
                .map(dto -> CartItemDto.builder()
                        .item(dto)
                        .count(ci.getCount())
                        .price(ci.getPrice())
                        .build());
    }

    private BigDecimal calculateTotal(List<CartItemDto> items) {
        return items.stream()
                .map(i -> i.price().multiply(BigDecimal.valueOf(i.count())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isUserAuthenticated(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }

    private Mono<Void> recalcAndSaveCartTotal(Long cartId) {
        return cartItemRepository.findAllByCartId(cartId)
                .map(ci -> ci.getPrice().multiply(BigDecimal.valueOf(ci.getCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .flatMap(total -> updateCartTotal(cartId, total))
                .then();
    }

    private Mono<Cart> updateCartTotal(Long cartId, BigDecimal total) {
        return cartRepository.findById(cartId)
                .flatMap(cart -> {
                    cart.setTotal(total);
                    return cartRepository.save(cart);
                });
    }

    private ItemDto toDeletedItemDto(Long itemId) {
        return new ItemDto(
                itemId, null, null, null, null, 0);
    }
}
