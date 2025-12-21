package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.enums.Action;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
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
import io.github.habatoo.store.payment.api.PaymentsApi;
import io.github.habatoo.store.payment.model.BalanceResponse;
import io.github.habatoo.store.payment.model.PaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit-тесты CartServiceImpl")
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ItemMapper itemMapper;
    @Mock
    private PaymentsApi paymentsApi;

    @InjectMocks
    private CartServiceImpl service;

    private final String MOCK_EXTERNAL_ID = "test-sub";
    private User mockUser;
    private Cart mockCart;
    private CartItem mockCartItem;
    private Item mockItem;
    private ItemDto mockItemDto;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(100L);
        mockUser.setExternalId(MOCK_EXTERNAL_ID);

        mockCart = new Cart();
        mockCart.setId(7L);
        mockCart.setUserId(100L);

        mockItem = new Item();
        mockItem.setId(30L);
        mockItem.setPrice(BigDecimal.TEN);

        mockCartItem = new CartItem();
        mockCartItem.setCartId(7L);
        mockCartItem.setItemId(30L);
        mockCartItem.setCount(1);
        mockCartItem.setPrice(BigDecimal.TEN);

        mockItemDto = new ItemDto(30L, null, null, null, BigDecimal.TEN, 1);
    }

    /**
     * Тестирование уменьшения количества товара, когда он остается в корзине.
     */
    @Test
    @DisplayName("Уменьшение количества: товар остается в корзине")
    void changeNumberOfItemsDecrementStaysTest() {
        ChangeNumberOfItemsRequestDto req = createRequest(20L, Action.MINUS);

        mockCartItem.setItemId(20L);
        mockCartItem.setCount(2);
        mockCartItem.setPrice(BigDecimal.valueOf(50));
        mockItemDto = new ItemDto(20L, null, null, null, BigDecimal.valueOf(50), 1);

        mockAuthAndCart(5L);

        when(cartItemRepository.findAllByCartId(5L)).thenReturn(Flux.just(mockCartItem));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(Mono.just(mockCartItem));
        when(itemRepository.findById(20L)).thenReturn(Mono.just(mockItem));
        when(itemMapper.toDto(any(Item.class))).thenReturn(mockItemDto);

        StepVerifier.create(withAuth(service.changeNumberOfItems(req)))
                .expectNext(mockItemDto)
                .verifyComplete();

        verify(cartItemRepository).save(argThat(c -> c.getCount() == 1));
    }

    /**
     * Тестирование поведения системы при отсутствии авторизации.
     */
    @Test
    @DisplayName("Изменение количества: отказ для неавторизованного пользователя")
    void changeNumberOfItemsUnauthTest() {
        ChangeNumberOfItemsRequestDto req = createRequest(1L, Action.PLUS);

        StepVerifier.create(service.changeNumberOfItems(req))
                .verifyComplete();
    }

    /**
     * Тестирование удаления товара из корзины при уменьшении количества до нуля.
     */
    @Test
    @DisplayName("Уменьшение количества до нуля: удаление товара из корзины")
    void changeNumberOfItemsDecrementToZeroTest() {
        ChangeNumberOfItemsRequestDto req = createRequest(30L, Action.MINUS);
        mockItemDto = new ItemDto(30L, null, null, null, null, 0);

        mockAuthAndCart(7L);
        when(cartItemRepository.findAllByCartId(7L)).thenReturn(Flux.just(mockCartItem), Flux.empty());
        when(cartItemRepository.delete(mockCartItem)).thenReturn(Mono.empty());
        when(cartRepository.findById(7L)).thenReturn(Mono.just(mockCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(Mono.just(mockCart));
        when(itemRepository.findById(30L)).thenReturn(Mono.just(mockItem));
        when(itemMapper.toDto(any(Item.class))).thenReturn(mockItemDto);

        StepVerifier.create(withAuth(service.changeNumberOfItems(req)))
                .expectNext(mockItemDto)
                .verifyComplete();

        verify(cartItemRepository).delete(mockCartItem);
        verify(cartRepository).save(argThat(c -> c.getTotal().compareTo(BigDecimal.ZERO) == 0));
    }

    /**
     * Тестирование получения списка всех товаров в корзине.
     */
    @Test
    @DisplayName("Получение содержимого корзины")
    void getItemsInTheCartTest() {
        mockAuthAndCart(1L);
        when(cartItemRepository.findAllByCartId(1L)).thenReturn(Flux.just(mockCartItem));
        when(itemRepository.findById(anyLong())).thenReturn(Mono.just(mockItem));
        when(itemMapper.toDto(any(Item.class))).thenReturn(mockItemDto);

        StepVerifier.create(withAuth(service.getItemsInTheCart()))
                .expectNextMatches(result -> result.id().equals(1L) && result.items().size() == 1)
                .verifyComplete();
    }

    /**
     * Тестирование успешной проверки возможности оплаты при достаточном балансе.
     */
    @Test
    @DisplayName("Проверка оплаты: успех при достаточном балансе")
    void canProcessPaymentSuccessTest() {
        mockPaymentApi(BigDecimal.valueOf(300));
        StepVerifier.create(service.canProcessPayment(new PaymentRequest().amount(BigDecimal.valueOf(200))))
                .expectNext(true)
                .verifyComplete();
    }

    /**
     * Тестирование отказа в оплате при недостаточном балансе.
     */
    @Test
    @DisplayName("Проверка оплаты: отказ при недостаточном балансе")
    void canProcessPaymentWhenBalanceNotEnoughReturnFalseTest() {
        mockPaymentApi(BigDecimal.valueOf(100));
        StepVerifier.create(service.canProcessPayment(new PaymentRequest().amount(BigDecimal.valueOf(300))))
                .expectNext(false)
                .verifyComplete();
    }

    /**
     * Тестирование проверки оплаты, когда баланс равен сумме покупки.
     */
    @Test
    @DisplayName("Проверка оплаты: успех при балансе равном сумме")
    void canProcessPaymentWhenBalanceEqualsAmountReturnTrueTest() {
        mockPaymentApi(BigDecimal.valueOf(200));
        StepVerifier.create(service.canProcessPayment(new PaymentRequest().amount(BigDecimal.valueOf(200))))
                .expectNext(true)
                .verifyComplete();
    }

    /**
     * Тестирование обработки ошибок внешнего API платежной системы.
     */
    @Test
    @DisplayName("Проверка оплаты: обработка ошибки API")
    void canProcessPaymentApiErrorTest() {
        when(paymentsApi.getWalletBalance()).thenReturn(Mono.error(new RuntimeException()));
        StepVerifier.create(service.canProcessPayment(new PaymentRequest().amount(BigDecimal.valueOf(100))))
                .expectNext(false)
                .verifyComplete();
    }

    /**
     * Проверка регистрации нового пользователя и создания для него корзины при первом входе.
     */
    @Test
    @DisplayName("Синхронизация: регистрация нового пользователя и создание корзины")
    void syncUserAndCreateNewCartTest() {
        when(userRepository.findByExternalId(MOCK_EXTERNAL_ID)).thenReturn(Mono.empty());
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(mockUser));
        when(cartRepository.findByUserId(mockUser.getId())).thenReturn(Mono.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(Mono.just(mockCart));
        when(cartItemRepository.findAllByCartId(anyLong())).thenReturn(Flux.empty());

        StepVerifier.create(withAuth(service.getItemsInTheCart()))
                .expectNextMatches(result -> result.id().equals(7L) && result.items().isEmpty())
                .verifyComplete();

        verify(userRepository).save(argThat(u -> u.getExternalId().equals(MOCK_EXTERNAL_ID)));
        verify(cartRepository).save(argThat(c -> c.getUserId().equals(100L)));
    }

    /**
     * Проверка извлечения данных из OAuth2AuthenticationToken (OidcUser).
     */
    @Test
    @DisplayName("Аутентификация: использование OAuth2AuthenticationToken")
    void oauth2AuthenticationSupportTest() {
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getAttribute("sub")).thenReturn(MOCK_EXTERNAL_ID);

        OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(oidcUser, null, "client-id");

        when(userRepository.findByExternalId(MOCK_EXTERNAL_ID)).thenReturn(Mono.just(mockUser));
        when(cartRepository.findByUserId(mockUser.getId())).thenReturn(Mono.just(mockCart));
        when(cartItemRepository.findAllByCartId(anyLong())).thenReturn(Flux.empty());

        StepVerifier.create(service.getItemsInTheCart()
                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(createMockContext(auth)))))
                .expectNextCount(1)
                .verifyComplete();
    }

    /**
     * Проверка добавления нового товара, которого еще нет в корзине.
     */
    @Test
    @DisplayName("Изменение количества: добавление нового товара (handleNewItem)")
    void handleNewItemActionPlusTest() {
        ChangeNumberOfItemsRequestDto req = createRequest(30L, Action.PLUS);

        mockAuthAndCart(7L);
        when(cartItemRepository.findAllByCartId(7L)).thenReturn(Flux.empty(), Flux.just(mockCartItem));
        when(itemRepository.findById(30L)).thenReturn(Mono.just(mockItem));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(Mono.just(mockCartItem));
        when(cartRepository.findById(7L)).thenReturn(Mono.just(mockCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(Mono.just(mockCart));
        when(itemMapper.toDto(any(Item.class))).thenReturn(mockItemDto);

        StepVerifier.create(withAuth(service.changeNumberOfItems(req)))
                .expectNext(mockItemDto)
                .verifyComplete();

        verify(cartItemRepository).save(argThat(ci -> ci.getItemId().equals(30L) && ci.getCount() == 1));
    }

    /**
     * Проверка игнорирования действия MINUS для нового товара.
     */
    @Test
    @DisplayName("Изменение количества: игнорирование MINUS для нового товара")
    void handleNewItemActionMinusIgnoreTest() {
        ChangeNumberOfItemsRequestDto req = createRequest(30L, Action.MINUS);

        mockAuthAndCart(7L);
        when(cartItemRepository.findAllByCartId(7L)).thenReturn(Flux.empty());

        StepVerifier.create(withAuth(service.changeNumberOfItems(req)))
                .verifyComplete();
    }

    /**
     * Проверка цепочки вызовов при изменении количества прямо из корзины.
     */
    @Test
    @DisplayName("Изменение количества из корзины: полная цепочка")
    void changeNumberOfItemsFromCartTest() {
        ChangeNumberOfItemsRequestDto req = createRequest(30L, Action.PLUS);

        mockAuthAndCart(7L);
        when(cartItemRepository.findAllByCartId(7L)).thenReturn(Flux.just(mockCartItem));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(Mono.just(mockCartItem));
        when(itemRepository.findById(30L)).thenReturn(Mono.just(mockItem));
        when(itemMapper.toDto(any(Item.class))).thenReturn(mockItemDto);

        StepVerifier.create(withAuth(service.changeNumberOfItemsFromCart(req)))
                .expectNextMatches(res -> res.id().equals(7L))
                .verifyComplete();
    }

    /**
     * Проверка возврата заглушки при удалении товара, который более не существует в БД.
     */
    @Test
    @DisplayName("Удаление: возврат DTO для удаленного из БД товара")
    void removeItemWithMissingItemInDbTest() {
        ChangeNumberOfItemsRequestDto req = createRequest(30L, Action.MINUS);

        mockAuthAndCart(7L);
        when(cartItemRepository.findAllByCartId(7L)).thenReturn(Flux.just(mockCartItem), Flux.empty());
        when(cartItemRepository.delete(any(CartItem.class))).thenReturn(Mono.empty());
        when(cartRepository.findById(7L)).thenReturn(Mono.just(mockCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(Mono.just(mockCart));
        when(itemRepository.findById(30L)).thenReturn(Mono.empty());

        StepVerifier.create(withAuth(service.changeNumberOfItems(req)))
                .expectNextMatches(dto -> dto.id().equals(30L) && dto.count() == 0)
                .verifyComplete();
    }

    @Test
    @DisplayName("Аутентификация: некорректный тип Principal")
    void extractExternalIdNullTest() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(new Object());

        StepVerifier.create(service.getItemsInTheCart()
                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(createMockContext(auth)))))
                .verifyComplete();
    }

    private SecurityContext createMockContext(Authentication auth) {
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        return context;
    }

    private void mockAuthAndCart(Long cartId) {
        mockCart.setId(cartId);
        when(userRepository.findByExternalId(MOCK_EXTERNAL_ID)).thenReturn(Mono.just(mockUser));
        when(cartRepository.findByUserId(mockUser.getId())).thenReturn(Mono.just(mockCart));
    }

    private void mockPaymentApi(BigDecimal balance) {
        BalanceResponse res = new BalanceResponse().balance(balance);
        when(paymentsApi.getWalletBalance()).thenReturn(Mono.just(res));
    }

    private ChangeNumberOfItemsRequestDto createRequest(Long id, Action action) {
        return ChangeNumberOfItemsRequestDto.builder().id(id).action(action).build();
    }

    private <T> Mono<T> withAuth(Mono<T> publisher) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", MOCK_EXTERNAL_ID)
                .build();

        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
        auth.setAuthenticated(true);
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);

        return publisher.contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context)));
    }
}
