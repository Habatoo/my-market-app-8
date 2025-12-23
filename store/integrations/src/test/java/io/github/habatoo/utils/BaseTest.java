package io.github.habatoo.utils;

import io.github.habatoo.entity.*;
import io.github.habatoo.repositories.*;
import io.github.habatoo.store.payment.api.PaymentsApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.security.oauth2.client.registration.test.client-id=test-client",
                "spring.security.oauth2.client.registration.test.client-secret=test-secret",
                "spring.security.oauth2.client.registration.test.authorization-grant-type=authorization_code",
                "spring.security.oauth2.client.registration.test.redirect-uri=http://localhost:8080/login/oauth2/code/keycloak",
                "spring.security.oauth2.client.registration.test.scope=openid,profile",

                "spring.security.oauth2.client.provider.test.authorization-uri=http://localhost:8080/auth",
                "spring.security.oauth2.client.provider.test.token-uri=http://localhost:8080/token",
                "spring.security.oauth2.client.provider.test.jwk-set-uri=http://localhost:8080/jwks",
                "spring.security.oauth2.client.provider.test.user-info-uri=http://localhost:8080/userinfo"
        }
)
@Testcontainers
public abstract class BaseTest {

    @Autowired
    protected CartRepository cartRepository;

    @Autowired
    protected CartItemRepository cartItemRepository;

    @Autowired
    protected OrderItemRepository orderItemRepository;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected ItemRepository itemRepository;

    @Autowired
    protected UserRepository userRepository;

    @MockitoBean
    protected PaymentsApi paymentsApi;

    @BeforeEach
    void cleanUp() {
        cleanDataBase();
    }

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("shop_db")
            .withUsername("test")
            .withPassword("test");

    @Container
    @ServiceConnection
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.2.4-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void overrideOAuth2Properties(DynamicPropertyRegistry registry) {
        String kReg = "spring.security.oauth2.client.registration.keycloak";
        String kProv = "spring.security.oauth2.client.provider.keycloak";

        registry.add(kReg + ".client-id", () -> "test-client");
        registry.add(kReg + ".client-secret", () -> "test-secret");
        registry.add(kReg + ".authorization-grant-type", () -> "authorization_code");
        registry.add(kReg + ".redirect-uri", () -> "{baseUrl}/login/oauth2/code/{registrationId}");

        registry.add(kProv + ".authorization-uri", () -> "http://localhost:9999/auth");
        registry.add(kProv + ".token-uri", () -> "http://localhost:9999/token");
        registry.add(kProv + ".jwk-set-uri", () -> "http://localhost:9999/jwks");

        String tReg = "spring.security.oauth2.client.registration.test";
        String tProv = "spring.security.oauth2.client.provider.test";

        registry.add(tReg + ".client-id", () -> "test-client");
        registry.add(tReg + ".client-secret", () -> "test-secret");
        registry.add(tReg + ".authorization-grant-type", () -> "authorization_code");
        registry.add(tReg + ".redirect-uri", () -> "{baseUrl}/login/oauth2/code/{registrationId}");

        registry.add(tProv + ".authorization-uri", () -> "http://localhost:9999/auth");
        registry.add(tProv + ".token-uri", () -> "http://localhost:9999/token");
        registry.add(tProv + ".jwk-set-uri", () -> "http://localhost:9999/jwks");
    }

    /**
     * Создать и сохранить Cart с указанной суммой.
     */
    protected Mono<Cart> createAndSaveCart(BigDecimal total) {
        Cart cart = new Cart();
        cart.setTotal(total);
        return cartRepository.save(cart);
    }

    protected Mono<Cart> createAndSaveCart() {
        return createAndSaveCart(BigDecimal.ZERO);
    }

    protected Mono<Cart> createAndSaveCart(BigDecimal total, Long userId) {
        Cart cart = new Cart();
        cart.setTotal(total);
        cart.setUserId(userId);

        return cartRepository.save(cart);
    }

    protected Mono<User> createAndSaveUser() {
        User user = new User();
        user.setUsername("user");
        user.setExternalId(UUID.randomUUID().toString());
        user.setRole("USER");

        return userRepository.save(user);
    }

    /**
     * Создать и сохранить Item с указанными параметрами.
     */
    protected Mono<Item> createAndSaveItem(String title, BigDecimal price) {
        Item item = new Item();
        item.setTitle(title);
        item.setDescription("desc_" + title);
        item.setImgPath("img/" + title);
        item.setPrice(price);
        return itemRepository.save(item);
    }

    /**
     * Создать и сохранить CartItem для указанной корзины и товара.
     */
    protected Mono<CartItem> createAndSaveCartItem(Cart cart, Item item, int count, BigDecimal price) {
        CartItem cartItem = new CartItem();
        cartItem.setCartId(cart.getId());
        cartItem.setItemId(item.getId());
        cartItem.setCount(count);
        cartItem.setPrice(price);
        return cartItemRepository.save(cartItem);
    }

    /**
     * Создание и сохранение Order с указанной суммой и датой.
     */
    protected Mono<Order> createAndSaveOrder(BigDecimal totalSum, LocalDateTime dateTime) {
        Order order = new Order();
        order.setTotalSum(totalSum);
        order.setDateTime(dateTime);
        return orderRepository.save(order);
    }

    /**
     * Создание и сохранение OrderItem, связываем с orderId и itemId.
     */
    protected Mono<OrderItem> createAndSaveOrderItem(Order order, Item item, int count, BigDecimal price) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(order.getId());
        orderItem.setItemId(item.getId());
        orderItem.setCount(count);
        orderItem.setPrice(price);
        return orderItemRepository.save(orderItem);
    }

    private void cleanDataBase() {
        orderItemRepository.deleteAll()
                .then(orderRepository.deleteAll())
                .then(cartItemRepository.deleteAll())
                .then(cartRepository.deleteAll())
                .then(itemRepository.deleteAll())
                .block();
    }
}
