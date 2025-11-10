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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для CartServiceImpl — покрывают основные и граничные сценарии изменения количества товаров в корзине.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тест загрузки CartServiceImpl")
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private CartMapper cartMapper;
    @Mock
    private ItemMapper itemMapper;

    private CartServiceImpl cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartServiceImpl(
                cartRepository,
                cartItemRepository,
                itemRepository,
                cartMapper,
                itemMapper);
    }

    /**
     * Тест — увеличение количества товара; товар уже есть в корзине.
     */
    @Test
    @DisplayName("PLUS: увеличивает количество товара в корзине")
    void plusExistingItemTest() {
        Cart cart = getCartWithItem(15L, 1, BigDecimal.valueOf(100));
        Item item = getItem(15L, BigDecimal.valueOf(100));

        ChangeNumberOfItemsRequestDto request = ChangeNumberOfItemsRequestDto.builder()
                .id(15L)
                .action(Action.PLUS)
                .build();

        when(cartRepository.findAll()).thenReturn(List.of(cart));
        when(itemRepository.findById(15L)).thenReturn(Optional.of(item));
        when(itemMapper.toDto(item)).thenReturn(mock(ItemDto.class));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        ItemDto result = cartService.changeNumberOfItems(request);

        assertNotNull(result);
        verify(cartItemRepository).save(any(CartItem.class));
        verify(cartRepository).save(cart);
    }

    /**
     * Тест — уменьшение количества товара; товар останется после уменьшения.
     */
    @Test
    @DisplayName("MINUS: уменьшает количество товара, он остаётся в корзине")
    void minusItemCountAboveZeroTest() {
        Cart cart = getCartWithItem(25L, 2, BigDecimal.valueOf(50));
        Item item = getItem(25L, BigDecimal.valueOf(50));

        ChangeNumberOfItemsRequestDto request = ChangeNumberOfItemsRequestDto.builder()
                .id(25L)
                .action(Action.MINUS)
                .build();

        when(cartRepository.findAll()).thenReturn(List.of(cart));
        when(itemRepository.findById(25L)).thenReturn(Optional.of(item));
        when(itemMapper.toDto(item)).thenReturn(mock(ItemDto.class));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        cartService.changeNumberOfItems(request);

        verify(cartItemRepository).save(any(CartItem.class));
        verify(cartRepository).save(cart);
    }

    /**
     * Тест — уменьшение количества товара до нуля; товар должен быть удалён из корзины.
     */
    @Test
    @DisplayName("MINUS: уменьшает количество товара до нуля, товар удаляется из корзины")
    void minusItemCountToZeroTest() {
        Cart cart = getCartWithItem(35L, 1, BigDecimal.valueOf(60));
        Item item = getItem(35L, BigDecimal.valueOf(60));

        ChangeNumberOfItemsRequestDto request = ChangeNumberOfItemsRequestDto.builder()
                .id(35L)
                .action(Action.MINUS)
                .build();

        when(cartRepository.findAll()).thenReturn(List.of(cart));
        when(itemRepository.findById(35L)).thenReturn(Optional.of(item));
        when(itemMapper.toDto(item)).thenReturn(mock(ItemDto.class));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        cartService.changeNumberOfItems(request);

        verify(cartItemRepository).delete(any(CartItem.class));
        verify(cartRepository).save(cart);
    }

    /**
     * Тест — добавление нового товара (его нет в корзине).
     */
    @Test
    @DisplayName("PLUS: добавляет новый товар в корзину")
    void plusNewItemTest() {
        Cart cart = new Cart();
        cart.setItems(new ArrayList<>());
        Item item = getItem(44L, BigDecimal.valueOf(77));

        ChangeNumberOfItemsRequestDto request = ChangeNumberOfItemsRequestDto.builder()
                .id(44L)
                .action(Action.PLUS)
                .build();

        when(cartRepository.findAll()).thenReturn(List.of(cart));
        when(itemRepository.findById(44L)).thenReturn(Optional.of(item));
        when(itemMapper.toDto(item)).thenReturn(mock(ItemDto.class));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        cartService.changeNumberOfItems(request);

        verify(cartItemRepository).save(any(CartItem.class));
        verify(cartRepository).save(cart);
    }

    /**
     * Тест — товар не найден — выбрасывается IllegalStateException.
     */
    @Test
    @DisplayName("Выбрасывает исключение при отсутствии товара")
    void itemNotFoundTest() {
        Cart cart = new Cart();
        cart.setItems(new ArrayList<>());
        ChangeNumberOfItemsRequestDto request = ChangeNumberOfItemsRequestDto.builder()
                .id(999L)
                .action(Action.PLUS)
                .build();

        when(cartRepository.findAll()).thenReturn(List.of(cart));
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> cartService.changeNumberOfItems(request));
    }

    /**
     * Тест получения корзины — преобразует Cart в CartDto.
     */
    @Test
    @DisplayName("Получает корзину и преобразует в DTO")
    void getItemsInTheCartTest() {
        Cart cart = new Cart();
        cart.setItems(new ArrayList<>());
        CartDto dto = mock(CartDto.class);

        when(cartRepository.findAll()).thenReturn(List.of(cart));
        when(cartMapper.toDto(cart)).thenReturn(dto);

        CartDto result = cartService.getItemsInTheCart();

        assertEquals(dto, result);
        verify(cartMapper).toDto(cart);
    }

    private Cart getCartWithItem(Long itemId, int count, BigDecimal price) {
        Cart cart = new Cart();
        Item item = getItem(itemId, price);
        CartItem cartItem = new CartItem();
        cartItem.setItem(item);
        cartItem.setCount(count);
        cartItem.setPrice(price);
        cart.setItems(new ArrayList<>(List.of(cartItem)));
        return cart;
    }

    private Item getItem(Long id, BigDecimal price) {
        Item item = new Item();
        item.setId(id);
        item.setPrice(price);
        return item;
    }
}
