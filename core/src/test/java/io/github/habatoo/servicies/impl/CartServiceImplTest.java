package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.enums.Action;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.response.CartItemDto;
import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.entity.Cart;
import io.github.habatoo.entity.CartItem;
import io.github.habatoo.entity.Item;
import io.github.habatoo.mappers.ItemMapper;
import io.github.habatoo.repositories.CartItemRepository;
import io.github.habatoo.repositories.CartRepository;
import io.github.habatoo.repositories.ItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private ItemMapper itemMapper;
    @InjectMocks
    private CartServiceImpl service;

    /**
     * Корзина отсутствует (findAll().next() → empty) → создаётся новая корзина.
     * Затем выполняется добавление нового товара (+1), т.к. товара ещё нет в корзине.
     */
    @Test
    @DisplayName("changeNumberOfItems — корзина отсутствует, создаётся новая и добавляется первый товар")
    void testChangeNumberOfItemsCartNotExists() {
        ChangeNumberOfItemsRequestDto req =
                ChangeNumberOfItemsRequestDto.builder()
                        .id(10L)
                        .action(Action.PLUS)
                        .build();

        Cart newCart = new Cart();
        newCart.setId(1L);

        Item item = new Item();
        item.setId(10L);
        item.setPrice(BigDecimal.TEN);

        ItemDto itemDto = new ItemDto(10L, null, null, null, BigDecimal.TEN, 1);

        when(cartRepository.findAll()).thenReturn(Flux.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(Mono.just(newCart));
        when(itemRepository.findById(10L)).thenReturn(Mono.just(item));
        when(cartItemRepository.findAllByCartId(1L)).thenReturn(Flux.empty());
        when(cartItemRepository.save(any(CartItem.class)))
                .thenReturn(Mono.just(new CartItem()));
        when(cartRepository.findById(1L)).thenReturn(Mono.just(newCart));
        when(itemMapper.toDto(item)).thenReturn(itemDto);

        StepVerifier.create(service.changeNumberOfItems(req))
                .expectNext(itemDto)
                .verifyComplete();

        verify(cartRepository, times(2)).save(any(Cart.class));
        verify(cartItemRepository).save(any(CartItem.class));
        verify(cartRepository, atLeastOnce()).findById(1L);
    }

    /**
     * Товар уже находится в корзине, action=PLUS → count увеличивается.
     */
    @Test
    @DisplayName("changeNumberOfItems — товар найден, increment")
    void testChangeNumberOfItemsIncrement() {
        Cart cart = new Cart();
        cart.setId(5L);

        CartItem ci = new CartItem();
        ci.setCartId(5L);
        ci.setItemId(20L);
        ci.setCount(2);
        ci.setPrice(BigDecimal.valueOf(50));

        Item item = new Item();
        item.setId(20L);
        item.setPrice(BigDecimal.valueOf(50));

        ItemDto dto = new ItemDto(20L, null, null, null, BigDecimal.valueOf(50), 1);

        ChangeNumberOfItemsRequestDto req =
                ChangeNumberOfItemsRequestDto.builder()
                        .id(20L)
                        .action(Action.PLUS)
                        .build();

        when(cartRepository.findAll()).thenReturn(Flux.just(cart));
        when(cartItemRepository.findAllByCartId(5L)).thenReturn(Flux.just(ci));
        when(cartItemRepository.save(any())).thenReturn(Mono.just(ci));
        when(itemRepository.findById(20L)).thenReturn(Mono.just(item));
        when(itemMapper.toDto(item)).thenReturn(dto);

        StepVerifier.create(service.changeNumberOfItems(req))
                .expectNext(dto)
                .verifyComplete();

        verify(cartItemRepository).save(argThat(c -> c.getCount() == 3));
    }

    /**
     * Товар в корзине, action=MINUS → count уменьшается, но остаётся > 0.
     */
    @Test
    @DisplayName("changeNumberOfItems — decrement, товар остаётся в корзине")
    void testChangeNumberOfItemsDecrementStays() {
        Cart cart = new Cart();
        cart.setId(5L);

        CartItem ci = new CartItem();
        ci.setCartId(5L);
        ci.setItemId(20L);
        ci.setCount(2);
        ci.setPrice(BigDecimal.valueOf(50));

        Item item = new Item();
        item.setId(20L);

        ItemDto dto = new ItemDto(20L, null, null, null, BigDecimal.valueOf(50), 1);

        ChangeNumberOfItemsRequestDto req =
                ChangeNumberOfItemsRequestDto.builder()
                        .id(20L)
                        .action(Action.MINUS)
                        .build();

        when(cartRepository.findAll()).thenReturn(Flux.just(cart));
        when(cartItemRepository.findAllByCartId(5L)).thenReturn(Flux.just(ci));
        when(cartItemRepository.save(any())).thenReturn(Mono.just(ci));
        when(itemRepository.findById(20L)).thenReturn(Mono.just(item));
        when(itemMapper.toDto(item)).thenReturn(dto);

        StepVerifier.create(service.changeNumberOfItems(req))
                .expectNext(dto)
                .verifyComplete();

        verify(cartItemRepository).save(argThat(c -> c.getCount() == 1));
    }

    /**
     * count становится 0 → CartItem удаляется, выполняется пересчёт тотала.
     */
    @Test
    @DisplayName("changeNumberOfItems — decrement до 0 → удаление товара")
    void testChangeNumberOfItemsDecrementToZero() {
        Cart cart = new Cart();
        cart.setId(7L);

        CartItem ci = new CartItem();
        ci.setCartId(7L);
        ci.setItemId(30L);
        ci.setCount(1);
        ci.setPrice(BigDecimal.TEN);

        ChangeNumberOfItemsRequestDto req =
                ChangeNumberOfItemsRequestDto.builder()
                        .id(30L)
                        .action(Action.MINUS)
                        .build();

        when(cartRepository.findAll())
                .thenReturn(Flux.just(cart));
        when(cartItemRepository.findAllByCartId(7L))
                .thenReturn(Flux.just(ci))
                .thenReturn(Flux.empty());
        when(cartItemRepository.delete(ci))
                .thenReturn(Mono.empty());
        when(cartRepository.findById(7L))
                .thenReturn(Mono.just(cart));
        when(cartRepository.save(any()))
                .thenReturn(Mono.just(cart));

        Item item = new Item();
        item.setId(30L);
        item.setPrice(BigDecimal.TEN);
        when(itemRepository.findById(30L)).thenReturn(Mono.just(item));

        ItemDto dto = new ItemDto(30L, "title", "desc", "img/path", BigDecimal.TEN, 0);
        when(itemMapper.toDto(item)).thenReturn(dto);

        StepVerifier.create(service.changeNumberOfItems(req))
                .expectNext(dto)
                .verifyComplete();

        verify(cartItemRepository).delete(ci);
        verify(cartRepository).save(argThat(c ->
                c.getTotal().compareTo(BigDecimal.ZERO) == 0
        ));
    }

    /**
     * Товар отсутствует в корзине, action=PLUS → создаётся новый CartItem.
     */
    @Test
    @DisplayName("changeNumberOfItems — increment товара, которого нет в корзине")
    void testChangeNumberOfItemsIncrementNewItem() {
        Cart cart = new Cart();
        cart.setId(3L);

        Item item = new Item();
        item.setId(50L);
        item.setPrice(BigDecimal.valueOf(20));

        ItemDto dto = new ItemDto(50L, null, null, null, BigDecimal.valueOf(20), 1);

        ChangeNumberOfItemsRequestDto req =
                ChangeNumberOfItemsRequestDto.builder()
                        .id(50L)
                        .action(Action.PLUS)
                        .build();

        when(cartRepository.findAll()).thenReturn(Flux.just(cart));
        when(cartItemRepository.findAllByCartId(3L)).thenReturn(Flux.empty());

        when(itemRepository.findById(50L)).thenReturn(Mono.just(item));
        when(cartItemRepository.save(any())).thenReturn(Mono.just(new CartItem()));

        when(cartItemRepository.findAllByCartId(3L)).thenReturn(Flux.empty());
        when(cartRepository.findById(3L)).thenReturn(Mono.just(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(Mono.just(cart));

        when(itemMapper.toDto(item)).thenReturn(dto);

        StepVerifier.create(service.changeNumberOfItems(req))
                .expectNext(dto)
                .verifyComplete();
    }

    /**
     * getItemsInTheCart — сбор DTO
     */
    @Test
    @DisplayName("getItemsInTheCart — корректная сборка CartDto")
    void testGetItemsInTheCart() {
        Cart cart = new Cart();
        cart.setId(1L);

        CartItem ci = new CartItem();
        ci.setCartId(1L);
        ci.setItemId(10L);
        ci.setCount(2);
        ci.setPrice(BigDecimal.valueOf(30));

        Item item = new Item();
        item.setId(10L);
        item.setPrice(BigDecimal.valueOf(30));

        ItemDto itemDto = new ItemDto(10L, null, null, null, BigDecimal.valueOf(30), 1);
        CartItemDto expectedCI =
                new CartItemDto(itemDto, 2, BigDecimal.valueOf(30));

        when(cartRepository.findAll()).thenReturn(Flux.just(cart));
        when(cartItemRepository.findAllByCartId(1L)).thenReturn(Flux.just(ci));
        when(itemRepository.findById(10L)).thenReturn(Mono.just(item));
        when(itemMapper.toDto(item)).thenReturn(itemDto);

        StepVerifier.create(service.getItemsInTheCart())
                .expectNextMatches(result ->
                        result.id() == 1 &&
                                result.items().size() == 1 &&
                                result.items().get(0).count() == 2 &&
                                result.total().compareTo(BigDecimal.valueOf(60)) == 0
                )
                .verifyComplete();
    }

    /**
     * changeNumberOfItemsFromCart — вызывает changeNumberOfItems + getItemsInTheCart
     */
    @Test
    @DisplayName("changeNumberOfItemsFromCart — цепочка вызовов change + getItems")
    void testChangeNumberOfItemsFromCart() {
        ChangeNumberOfItemsRequestDto req = ChangeNumberOfItemsRequestDto.builder()
                .id(5L)
                .action(Action.PLUS)
                .build();

        Cart cart = new Cart();
        cart.setId(1L);

        when(cartRepository.findAll()).thenReturn(Flux.just(cart));
        when(cartItemRepository.findAllByCartId(1L)).thenReturn(Flux.empty());

        Item item = new Item();
        item.setId(5L);
        item.setPrice(BigDecimal.TEN);

        ItemDto dto = new ItemDto(5L, null, null, null, BigDecimal.TEN, 1);

        when(itemRepository.findById(5L)).thenReturn(Mono.just(item));
        when(itemMapper.toDto(item)).thenReturn(dto);
        when(cartItemRepository.save(any())).thenReturn(Mono.just(new CartItem()));
        when(cartRepository.findById(anyLong())).thenReturn(Mono.just(cart));
        when(cartRepository.save(any())).thenReturn(Mono.just(cart));

        StepVerifier.create(service.changeNumberOfItemsFromCart(req))
                .assertNext(res -> {
                    assertEquals(1L, res.id());
                    assertTrue(res.items().isEmpty(), "Корзина должна быть пустой");
                })
                .verifyComplete();

        verify(cartItemRepository, atLeastOnce()).save(any());
        verify(itemRepository).findById(5L);
    }

    /**
     * Имитирует ситуацию, когда из корзины удаляется последний товар (action = MINUS),
     * и это побочно вызывает recalcAndSaveCartTotal.changeNumberOfItems
     * — вызывает changeNumberOfItems + getItemsInTheCart
     */
    @Test
    @DisplayName("changeNumberOfItems — цепочка вызовов change + getItems + ")
    void testRecalcTriggeredOnItemDelete() {
        long cartId = 1L;
        long itemId = 100L;

        ChangeNumberOfItemsRequestDto request = ChangeNumberOfItemsRequestDto.builder()
                .id(itemId)
                .action(Action.MINUS)
                .sort(null)
                .pageNumber(null)
                .pageSize(null)
                .build();

        Cart cart = new Cart();
        cart.setId(cartId);

        when(cartRepository.findAll()).thenReturn(Flux.just(cart));
        when(cartRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        CartItem ci = new CartItem();
        ci.setCartId(cartId);
        ci.setItemId(itemId);
        ci.setCount(1);
        ci.setPrice(BigDecimal.TEN);

        CartItem ci2 = new CartItem();
        ci2.setCartId(cartId);
        ci2.setPrice(BigDecimal.valueOf(5));
        ci2.setCount(3);

        when(cartItemRepository.findAllByCartId(cartId))
                .thenReturn(Flux.just(ci))
                .thenReturn(Flux.just(ci2));
        when(cartItemRepository.delete(ci)).thenReturn(Mono.empty());
        when(cartRepository.findById(cartId)).thenReturn(Mono.just(cart));

        Item removedItem = new Item();
        removedItem.setId(itemId);
        removedItem.setTitle("title");
        removedItem.setDescription("desc");
        removedItem.setImgPath("img");
        removedItem.setPrice(BigDecimal.TEN);

        when(itemRepository.findById(itemId)).thenReturn(Mono.just(removedItem));
        when(itemMapper.toDto(removedItem)).thenReturn(new ItemDto(
                itemId, "title", "desc", "img", BigDecimal.TEN, 0));

        StepVerifier.create(service.changeNumberOfItems(request))
                .expectNextCount(1)
                .verifyComplete();

        verify(cartRepository).save(argThat(c ->
                c.getTotal().compareTo(BigDecimal.valueOf(15)) == 0
        ));
    }
}
