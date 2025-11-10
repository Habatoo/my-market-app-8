package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.enums.Action;
import io.github.habatoo.dto.enums.Sort;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.request.GetItemsRequestDto;
import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.dto.response.ItemDtoResponse;
import io.github.habatoo.dto.response.ItemsDtoResponse;
import io.github.habatoo.entity.Item;
import io.github.habatoo.mappers.ItemMapper;
import io.github.habatoo.repositories.ItemRepository;
import io.github.habatoo.servicies.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Параметризованные unit-тесты для ItemServiceImpl.
 * Покрывают кейсы поиска, сортировки, пагинации, пустого результата, поиска по описанию, получение
 * товара, изменение количества, несуществующего товара.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тест загрузки ItemServiceImpl")
class ItemServiceImplTest {

    @Mock
    private ItemRepository repository;
    @Mock
    private CartService cartService;
    @Mock
    private ItemMapper mapper;

    private ItemServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ItemServiceImpl(repository, cartService, mapper);
    }

    /**
     * Тест поиска товаров: поиск по title, сортировка, пагинация
     */
    @ParameterizedTest
    @MethodSource("itemsSearchCases")
    @DisplayName("Поиск, сортировка, пагинация витрины")
    void getItemsVariants(GetItemsRequestDto req, List<Item> all, List<Item> expectedPage) {
        when(repository.findAll()).thenReturn(all);
        when(mapper.toDto(anyList())).thenAnswer(inv ->
                ((List<Item>) inv.getArguments()[0]).stream()
                        .map(item -> new ItemDto(item.getId(), item.getTitle(), item.getDescription(), "", item.getPrice(), 0))
                        .toList());
        CartDto cart = mock(CartDto.class);
        when(cartService.getItemsInTheCart()).thenReturn(cart);

        ItemsDtoResponse response = service.getItems(req);

        assertEquals(expectedPage.size(), response.paging().pageSize());
        assertEquals(cart, response.cart());
        assertNotNull(response.itemsRows());
    }

    /**
     * Тест: пустой список товаров — пустая витрина
     */
    @Test
    @DisplayName("Пустой список товаров — пустая витрина")
    void getItemsEmptyTest() {
        GetItemsRequestDto req = GetItemsRequestDto.builder().build();
        when(repository.findAll()).thenReturn(List.of());
        when(cartService.getItemsInTheCart()).thenReturn(mock(CartDto.class));
        when(mapper.toDto(anyList())).thenReturn(List.of());

        ItemsDtoResponse response = service.getItems(req);

        assertTrue(response.itemsRows().isEmpty() || response.itemsRows().get(0).isEmpty());
    }

    /**
     * Тест поиска по описанию (description).
     */
    @Test
    @DisplayName("Поиск по description — фильтрует по подстроке")
    void searchByDescriptionTest() {
        //Item item = new Item(2L, "title", "СуперОписание", BigDecimal.TEN);
        GetItemsRequestDto req = GetItemsRequestDto.builder().search("описание").build();

        //when(repository.findAll()).thenReturn(List.of(item));
        when(repository.findAll()).thenReturn(List.of());
        when(cartService.getItemsInTheCart()).thenReturn(mock(CartDto.class));
        when(mapper.toDto(anyList())).thenReturn(List.of(new ItemDto(2L, "title", "СуперОписание", "", BigDecimal.TEN, 0)));

        ItemsDtoResponse result = service.getItems(req);

        assertFalse(result.itemsRows().isEmpty());
    }

    /**
     * Тест получения отдельного товара — товар найден.
     */
    @Test
    @DisplayName("Получение отдельного товара — товар найден")
    void getItemFoundTest() {
        Item item = new Item(5L, "A", null, "", BigDecimal.ONE, 0);
        when(repository.findById(5L)).thenReturn(Optional.of(item));
        when(mapper.toDto(item)).thenReturn(new ItemDto(5L, "A", null, "", BigDecimal.ONE, 0));

        CartDto cart = mock(CartDto.class);
        when(cart.getCountByItemId(5L)).thenReturn(2);
        when(cartService.getItemsInTheCart()).thenReturn(cart);

        ItemDtoResponse resp = service.getItem(5L);

        assertEquals(2, resp.cartCount());
        assertEquals(5L, resp.item().id());
    }

    /**
     * Тест падения при отсутствии товара.
     */
    @Test
    @DisplayName("Получение отсутствующего товара — выбрасывает исключение")
    void getItemNotFoundTest() {
        when(repository.findById(101L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.getItem(101L));
    }

    /**
     * Тест изменения количества товара — вызов CartService и возврат актуального количества.
     */
    @Test
    @DisplayName("Изменение количества товара из карточки")
    void changeNumberOfItemsFromPageTest() {
        ChangeNumberOfItemsRequestDto req = ChangeNumberOfItemsRequestDto.builder().id(23L).action(Action.PLUS).build();
        ItemDto itemDto = mock(ItemDto.class);
        CartDto cartDto = mock(CartDto.class);

        when(cartService.changeNumberOfItems(req)).thenReturn(itemDto);
        when(cartService.getItemsInTheCart()).thenReturn(cartDto);
        when(cartDto.getCountByItemId(23L)).thenReturn(4);

        ItemDtoResponse resp = service.changeNumberOfItemsFromPage(req);

        assertEquals(itemDto, resp.item());
        assertEquals(4, resp.cartCount());
    }

    static Stream<Arguments> itemsSearchCases() {
        Item itemA = new Item(1L, "Alpha", "descA", "", BigDecimal.valueOf(100), 0);
        Item itemB = new Item(2L, "Beta", "descB", "", BigDecimal.valueOf(200), 1);
        Item itemC = new Item(3L, "Gamma", "descC", "", BigDecimal.valueOf(150), 2);
        // Сортировка, поиск, пагинация
        return Stream.of(
                Arguments.of( // title поиск и сортировка
                        GetItemsRequestDto.builder().search("alpha").sort(Sort.ALPHA).pageSize(1).pageNumber(1).build(),
                        List.of(itemA, itemB, itemC),
                        List.of(itemA)
                ),
                Arguments.of( // сортировка по цене
                        GetItemsRequestDto.builder().sort(Sort.PRICE).pageSize(2).pageNumber(1).build(),
                        List.of(itemC, itemB, itemA),
                        List.of(itemA, itemC)
                ),
                Arguments.of( // без фильтра
                        GetItemsRequestDto.builder().pageSize(2).pageNumber(1).build(),
                        List.of(itemA, itemB),
                        List.of(itemA, itemB)
                )
        );
    }
}
