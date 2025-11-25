//package io.github.habatoo.repositories;
//
//import io.github.habatoo.entity.Cart;
//import io.github.habatoo.entity.CartItem;
//import io.github.habatoo.entity.Item;
//import io.github.habatoo.utils.BaseTest;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
//import org.springframework.context.annotation.Import;
//import reactor.core.publisher.Mono;
//import reactor.test.StepVerifier;
//
//import java.math.BigDecimal;
//
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
///**
// * Интеграционные тесты для CartRepository.
// * Проверяет сохранение, поиск и удаление корзины, а также корректность хранения суммы и наличия позиций.
// */
//@DataR2dbcTest
//@Import({CartRepository.class, CartItemRepository.class, ItemRepository.class})
//@DisplayName("Reactive интеграционные тесты CartRepository")
//class CartRepositoryIntegrationTest extends BaseTest {
//
//    @Autowired
//    private CartRepository cartRepository;
//
//    @Autowired
//    private CartItemRepository cartItemRepository;
//
//    @Autowired
//    private ItemRepository itemRepository;
//
//    @Test
//    @DisplayName("Сохранение и выборка корзины по id")
//    void findSavedCartByIdTest() {
//        Cart cart = createCart(BigDecimal.valueOf(100));
//
//        Mono<Cart> saved = cartRepository.save(cart);
//
//        StepVerifier.create(saved.flatMap(c -> cartRepository.findById(c.getId())))
//                .assertNext(found -> {
//                    assert found.getTotal().compareTo(BigDecimal.valueOf(100)) == 0;
//                })
//                .verifyComplete();
//    }
//
//    @Test
//    @DisplayName("Сохранение корзины с позициями и проверка связей")
//    void saveCartWithItemsTest() {
//        Cart cart = createCart(BigDecimal.valueOf(50));
//        Item item = createItem("CartRepoItem", BigDecimal.valueOf(25));
//        CartItem cartItem = new CartItem();
//        cartItem.setCount(2);
//        cartItem.setPrice(BigDecimal.valueOf(25));
//
//        Mono<Cart> testFlow = itemRepository.save(item)
//                .then(cartRepository.save(cart))
//                .flatMap(savedCart -> cartItemRepository.save(cartItem)
//                        .then(Mono.just(savedCart)))
//                .flatMap(savedCart -> cartRepository.findById(savedCart.getId()));
//
//        StepVerifier.create(testFlow)
//                .assertNext(found -> {
//                   // assertTrue(found.getItems().size() == 1);
//                    //CartItem foundItem = found.().get(0);
//                    //assertTrue(foundItem.getItem().getTitle().equals("CartRepoItem"));
//                   // assertTrue(foundItem.getCount() == 2);
//                })
//                .verifyComplete();
//    }
//
//    @Test
//    @DisplayName("Удаление корзины и проверка, что она не найдена")
//    void deleteCartByIdTest() {
//        Cart cart = createCart(BigDecimal.ZERO);
//
//        Mono<Long> testFlow = cartRepository.save(cart)
//                .flatMap(savedCart -> cartRepository.deleteById(savedCart.getId()).thenReturn(savedCart.getId()))
//                .flatMap(id -> cartRepository.findById(id).map(c -> 1L).defaultIfEmpty(0L));
//
//        StepVerifier.create(testFlow)
//                .expectNext(0L)
//                .verifyComplete();
//    }
//
//    @Test
//    @DisplayName("Поиск всех корзин")
//    void findAllCartsTest() {
//        Mono<Void> setup = cartRepository.save(createCart(BigDecimal.valueOf(10)))
//                .then(cartRepository.save(createCart(BigDecimal.valueOf(50))))
//                .then();
//
//        StepVerifier.create(setup.thenMany(cartRepository.findAll()).collectList())
//                .assertNext(carts -> assertTrue(carts.size() == 2))
//                .verifyComplete();
//    }
//
//    private Cart createCart(BigDecimal total) {
//        Cart cart = new Cart();
//        cart.setTotal(total);
//        return cart;
//    }
//
//    private Item createItem(String title, BigDecimal price) {
//        Item item = new Item();
//        item.setTitle(title);
//        item.setPrice(price);
//        return item;
//    }
//}
