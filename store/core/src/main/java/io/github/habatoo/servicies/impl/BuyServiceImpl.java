package io.github.habatoo.servicies.impl;

import io.github.habatoo.entity.Cart;
import io.github.habatoo.entity.CartItem;
import io.github.habatoo.entity.Order;
import io.github.habatoo.entity.OrderItem;
import io.github.habatoo.exceptions.InsufficientFundsException;
import io.github.habatoo.exceptions.PaymentServiceUnavailableException;
import io.github.habatoo.repositories.CartItemRepository;
import io.github.habatoo.repositories.CartRepository;
import io.github.habatoo.repositories.OrderItemRepository;
import io.github.habatoo.repositories.OrderRepository;
import io.github.habatoo.servicies.BuyService;
import io.github.habatoo.store.payment.api.PaymentsApi;
import io.github.habatoo.store.payment.model.PaymentRequest;
import io.github.habatoo.store.payment.model.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Реализация для осуществления покупки.
 * Предоставляет бизнес-логику для совершения покупки.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuyServiceImpl implements BuyService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final PaymentsApi paymentsApi;

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public Mono<Long> buy(Long cartId) {
        return findCartOrError(cartId)
                .flatMap(cart -> loadItemsOrError(cart.getId())
                        .flatMap(cartItems -> {
                            BigDecimal totalAmount = calculateTotalAmount(cartItems);
                            return processPayment(totalAmount)
                                    .flatMap(paymentStatus -> persistOrderAndCleanup(cart, cartItems, totalAmount));
                        })
                );
    }

    /**
     * Ищет корзину по идентификатору.
     *
     * @param id идентификатор корзины.
     * @return {@link Mono} с корзиной или ошибка, если корзина не найдена.
     */
    private Mono<Cart> findCartOrError(Long id) {
        return cartRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalStateException("Корзина с id=" + id + " не найдена")));
    }

    /**
     * Загружает список товаров для указанной корзины.
     *
     * @param cartId идентификатор корзины.
     * @return {@link Mono} со списком товаров.
     * @throws IllegalStateException если список товаров пуст.
     */
    private Mono<List<CartItem>> loadItemsOrError(Long cartId) {
        return cartItemRepository.findAllByCartId(cartId)
                .collectList()
                .flatMap(items -> {
                    if (items.isEmpty()) {
                        return Mono.error(new IllegalStateException("В корзине нет товаров для покупки"));
                    }
                    return Mono.just(items);
                });
    }

    /**
     * Вычисляет общую стоимость товаров в корзине.
     *
     * @param cartItems список товаров корзины.
     * @return итоговая сумма (BigDecimal).
     */
    private BigDecimal calculateTotalAmount(List<CartItem> cartItems) {
        return cartItems.stream()
                .map(ci -> ci.getPrice().multiply(BigDecimal.valueOf(ci.getCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Выполняет обращение к внешнему сервису платежей (PaymentsApi).
     *
     * @param totalAmount итоговая сумма для списания.
     * @return {@link Mono} со строкой статуса (например, "SUCCESS") в случае успеха.
     * @throws InsufficientFundsException         если статус ответа не SUCCESS.
     * @throws PaymentServiceUnavailableException при любых сетевых ошибках или сбоях API.
     */
    private Mono<String> processPayment(BigDecimal totalAmount) {
        PaymentRequest request = new PaymentRequest().amount(totalAmount);

        return paymentsApi.createPayment("application/json", request)
                .flatMap(response -> {
                    if (response.getStatus() == PaymentResponse.StatusEnum.SUCCESS) {
                        return Mono.just("SUCCESS");
                    }
                    return Mono.error(new InsufficientFundsException());
                })
                .onErrorResume(ex -> {
                    if (ex instanceof InsufficientFundsException) {
                        return Mono.error(ex);
                    }
                    log.error("Ошибка обращения к сервису платежей: {}", ex.getMessage(), ex);
                    return Mono.error(new PaymentServiceUnavailableException());
                });
    }

    /**
     * Сохраняет данные заказа, позиции заказа и очищает корзину.
     * <p>
     * Ранее этот метод назывался getOrderId, что вводило в заблуждение,
     * так как метод выполняет активные действия по сохранению (Side Effects).
     *
     * @param cart        объект корзины.
     * @param cartItems   список товаров из корзины.
     * @param totalAmount общая сумма заказа.
     * @return {@link Mono} с идентификатором созданного заказа.
     */
    private Mono<Long> persistOrderAndCleanup(Cart cart,
                                              List<CartItem> cartItems,
                                              BigDecimal totalAmount) {
        Order newOrder = buildOrderEntity(totalAmount, cart.getUserId());

        return orderRepository.save(newOrder)
                .flatMap(savedOrder ->
                        saveOrderItems(savedOrder, cartItems)
                                .then(clearCart(cart.getId()))
                                .thenReturn(savedOrder.getId())
                );
    }

    /**
     * Сохраняет позиции заказа (OrderItems) в базу данных.
     *
     * @param savedOrder сохраненная сущность заказа (нужна для получения ID).
     * @param cartItems  список товаров из корзины для конвертации.
     * @return {@link Mono<Void>} по завершении операции.
     */
    private Mono<Void> saveOrderItems(Order savedOrder, List<CartItem> cartItems) {
        return Flux.fromIterable(cartItems)
                .map(cartItem -> mapToOrderItem(savedOrder, cartItem))
                .flatMap(orderItemRepository::save)
                .then();
    }

    /**
     * Очищает корзину: удаляет все товары и сбрасывает общую стоимость.
     *
     * @param cartId идентификатор корзины.
     * @return {@link Mono<Void>} по завершении операции.
     */
    private Mono<Void> clearCart(Long cartId) {
        return cartItemRepository.deleteAllByCartId(cartId)
                .then(resetCartTotal(cartId))
                .then();
    }

    /**
     * Сбрасывает поле total у корзины в 0.
     *
     * @param cartId идентификатор корзины.
     * @return {@link Mono} с обновленной корзиной.
     */
    private Mono<Cart> resetCartTotal(Long cartId) {
        return cartRepository.findById(cartId)
                .flatMap(cart -> {
                    cart.setTotal(BigDecimal.ZERO);
                    return cartRepository.save(cart);
                });
    }

    /**
     * Конвертирует элемент корзины в позицию заказа.
     *
     * @param savedOrder сохраненный заказ.
     * @param cartItem   элемент корзины.
     * @return сущность {@link OrderItem}.
     */
    private OrderItem mapToOrderItem(Order savedOrder, CartItem cartItem) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(savedOrder.getId());
        orderItem.setItemId(cartItem.getItemId());
        orderItem.setCount(cartItem.getCount());
        orderItem.setPrice(cartItem.getPrice());
        return orderItem;
    }

    /**
     * Фабричный метод для создания сущности заказа.
     *
     * @param totalAmount итоговая сумма.
     * @param userId      идентификатор пользователя.
     * @return новая сущность {@link Order}, готовая к сохранению.
     */
    private Order buildOrderEntity(BigDecimal totalAmount, Long userId) {
        Order order = new Order();
        order.setDateTime(LocalDateTime.now());
        order.setUserId(userId);
        order.setTotalSum(totalAmount);
        return order;
    }
}
