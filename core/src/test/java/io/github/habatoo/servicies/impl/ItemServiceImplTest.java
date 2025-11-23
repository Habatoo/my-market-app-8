//package io.github.habatoo.servicies.impl;
//
//import io.github.habatoo.dto.enums.Action;
//import io.github.habatoo.dto.enums.Sort;
//import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
//import io.github.habatoo.dto.request.GetItemsRequestDto;
//import io.github.habatoo.dto.response.CartDto;
//import io.github.habatoo.dto.response.ItemDto;
//import io.github.habatoo.dto.response.ItemDtoResponse;
//import io.github.habatoo.dto.response.ItemsDtoResponse;
//import io.github.habatoo.entity.Item;
//import io.github.habatoo.mappers.ItemMapper;
//import io.github.habatoo.repositories.CartItemRepository;
//import io.github.habatoo.repositories.ItemRepository;
//import io.github.habatoo.servicies.CartService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.Arguments;
//import org.junit.jupiter.params.provider.MethodSource;
//import org.junit.jupiter.params.provider.NullSource;
//import org.junit.jupiter.params.provider.ValueSource;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.math.BigDecimal;
//import java.util.List;
//import java.util.Optional;
//import java.util.stream.Stream;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
///**
// * Параметризованные unit-тесты для ItemServiceImpl.
// * Покрывают кейсы поиска, сортировки, пагинации, пустого результата, поиска по описанию, получение
// * товара, изменение количества, несуществующего товара.
// */
//@ExtendWith(MockitoExtension.class)
//@DisplayName("Тест загрузки ItemServiceImpl")
//class ItemServiceImplTest {
//
//    @Mock
//    private ItemRepository repository;
//    @Mock
//    private CartItemRepository cartItemRepository;
//    @Mock
//    private CartService cartService;
//    @Mock
//    private ItemMapper mapper;
//
//    private ItemServiceImpl service;
//
//    @BeforeEach
//    void setUp() {
//        service = new ItemServiceImpl(repository, cartItemRepository, cartService, mapper);
//    }
//
//    /**
//     * Тест поиска товаров: поиск по title, сортировка, пагинация
//     */
//    @ParameterizedTest
//    @MethodSource("itemsSearchCases")
//    @DisplayName("Поиск, сортировка, пагинация витрины с itemCounts")
//    void getItemsVariants(GetItemsRequestDto req, List<Item> all, List<Item> expectedPage) {
//        when(repository.findAll()).thenReturn(all);
//        when(mapper.toDto(anyList())).thenAnswer(inv -> {
//            Object arg = inv.getArgument(0);
//            if (arg instanceof List<?> list) {
//                @SuppressWarnings("unchecked")
//                List<Item> items = (List<Item>) list;
//                return items.stream()
//                        .map(item -> new ItemDto(item.getId(), item.getTitle(), item.getDescription(), "", item.getPrice(), 0))
//                        .toList();
//            }
//            throw new IllegalArgumentException("Argument for toDto is not a List<Item>");
//        });
//        CartDto cart = mock(CartDto.class);
//        when(cartService.getItemsInTheCart()).thenReturn(cart);
//
//        if (req.getSearch() == null) {
//            lenient().when(cartItemRepository.findCountByCartIdAndItemId(any(), any())).thenReturn(null);
//        }
//        when(cartService.getItemsInTheCart()).thenReturn(cart);
//
//        ItemsDtoResponse response = service.getItems(req);
//
//        List<Long> actualIds = response.itemsRows().stream()
//                .flatMap(List::stream)
//                .map(ItemDto::id)
//                .filter(id -> id != -1)
//                .toList();
//        List<Long> expectedIds = expectedPage.stream().map(Item::getId).toList();
//        assertEquals(expectedIds, actualIds);
//
//        int toFill = (response.itemsRows().size() * 3) - expectedIds.size();
//        long emptyCount = response.itemsRows().stream()
//                .flatMap(List::stream)
//                .filter(dto -> dto.id() == -1)
//                .count();
//        assertEquals(toFill, emptyCount);
//        assertEquals(cart, response.cart());
//        assertNotNull(response.paging());
//    }
//
//    /**
//     * Тест: пустой список товаров — пустая витрина
//     */
//    @Test
//    @DisplayName("Пустой список товаров — пустая витрина")
//    void getItemsEmptyTest() {
//        GetItemsRequestDto req = GetItemsRequestDto.builder().build();
//        when(repository.findAll()).thenReturn(List.of());
//        when(cartService.getItemsInTheCart()).thenReturn(mock(CartDto.class));
//        when(mapper.toDto(anyList())).thenReturn(List.of());
//
//        ItemsDtoResponse response = service.getItems(req);
//
//        assertTrue(response.itemsRows().isEmpty() || response.itemsRows().get(0).isEmpty());
//    }
//
//    /**
//     * Тест поиска по описанию (description).
//     */
//    @Test
//    @DisplayName("Поиск по description — фильтрует по подстроке")
//    void searchByDescriptionTest() {
//        GetItemsRequestDto req = GetItemsRequestDto.builder().search("описание").build();
//        when(repository.findAll()).thenReturn(List.of());
//        when(cartService.getItemsInTheCart()).thenReturn(mock(CartDto.class));
//        when(mapper.toDto(anyList())).thenReturn(List.of(new ItemDto(2L, "title", "СуперОписание", "", BigDecimal.TEN, 0)));
//
//        ItemsDtoResponse result = service.getItems(req);
//
//        assertFalse(result.itemsRows().isEmpty());
//    }
//
//    /**
//     * Тест получения отдельного товара — товар найден.
//     */
//    @ParameterizedTest
//    @NullSource
//    @ValueSource(ints = {2, 0})
//    @DisplayName("Получение отдельного товара — товар найден с разными cartCount")
//    void getItemFoundTest(Integer ans) {
//        Item item = new Item(5L, "A", null, "", BigDecimal.ONE, 0);
//        when(repository.findById(5L)).thenReturn(Optional.of(item));
//        when(mapper.toDto(item)).thenReturn(new ItemDto(5L, "A", null, "", BigDecimal.ONE, 0));
//
//        CartDto cart = mock(CartDto.class);
//        when(cartItemRepository.findCountByCartIdAndItemId(any(), eq(item.getId()))).thenReturn(ans);
//        when(cartService.getItemsInTheCart()).thenReturn(cart);
//
//        ItemDtoResponse resp = service.getItem(5L);
//
//        assertEquals(ans == null ? 0 : ans, resp.cartCount());
//        assertEquals(5L, resp.item().id());
//    }
//
//    /**
//     * Тест падения при отсутствии товара.
//     */
//    @Test
//    @DisplayName("Получение отсутствующего товара — выбрасывает исключение")
//    void getItemNotFoundTest() {
//        when(repository.findById(101L)).thenReturn(Optional.empty());
//
//        assertThrows(IllegalStateException.class, () -> service.getItem(101L));
//    }
//
//    /**
//     * Тест изменения количества товара — вызов CartService и возврат актуального количества.
//     */
//    @ParameterizedTest
//    @NullSource
//    @ValueSource(ints = {4, 0})
//    @DisplayName("Изменение количества товара из карточки")
//    void changeNumberOfItemsFromPageTest(Integer ans) {
//        ChangeNumberOfItemsRequestDto req = ChangeNumberOfItemsRequestDto.builder().id(23L).action(Action.PLUS).build();
//        ItemDto itemDto = mock(ItemDto.class);
//        CartDto cartDto = mock(CartDto.class);
//
//        when(cartService.changeNumberOfItems(req)).thenReturn(itemDto);
//        when(cartService.getItemsInTheCart()).thenReturn(cartDto);
//        when(cartItemRepository.findCountByCartIdAndItemId(any(), eq(23L))).thenReturn(ans);
//
//        ItemDtoResponse resp = service.changeNumberOfItemsFromPage(req);
//
//        assertEquals(itemDto, resp.item());
//        assertEquals(ans == null ? 0 : ans, resp.cartCount());
//    }
//
//    static Stream<Arguments> itemsSearchCases() {
//        Item itemA = new Item(1L, "Alpha", "descA", "", BigDecimal.valueOf(100), 0);
//        Item itemB = new Item(2L, "Beta", "descB", "", BigDecimal.valueOf(200), 1);
//        Item itemC = new Item(3L, "Gamma", "descC", "", BigDecimal.valueOf(150), 2);
//        Item itemD = new Item(4L, "NotMatched", "descAlpha", "", BigDecimal.valueOf(75), 3);
//        Item itemE = new Item(5L, "NoWay", null, "", BigDecimal.valueOf(300), null);
//
//        return Stream.of(
//                Arguments.of(
//                        GetItemsRequestDto.builder().search(null).sort(null).pageSize(2).pageNumber(1).build(),
//                        List.of(itemA, itemB),
//                        List.of(itemA, itemB)
//                ),
//                Arguments.of(
//                        GetItemsRequestDto.builder().search("").sort(null).pageSize(2).pageNumber(1).build(),
//                        List.of(itemA, itemB, itemC),
//                        List.of(itemA, itemB)
//                ),
//                Arguments.of(
//                        GetItemsRequestDto.builder().search("   ").sort(null).pageSize(2).pageNumber(1).build(),
//                        List.of(itemA, itemB, itemC),
//                        List.of(itemA, itemB)
//                ),
//                Arguments.of(
//                        GetItemsRequestDto.builder().search("alpha").sort(null).pageSize(2).pageNumber(1).build(),
//                        List.of(itemA, itemB, itemC),
//                        List.of(itemA)
//                ),
//                Arguments.of(
//                        GetItemsRequestDto.builder().search("descB").sort(null).pageSize(2).pageNumber(1).build(),
//                        List.of(itemA, itemB, itemC),
//                        List.of(itemB)
//                ),
//                Arguments.of(
//                        GetItemsRequestDto.builder().search("no_match").sort(null).pageSize(2).pageNumber(1).build(),
//                        List.of(itemA, itemB, itemC),
//                        List.of()
//                ),
//                Arguments.of(
//                        GetItemsRequestDto.builder().search(null).sort(Sort.ALPHA).pageSize(3).pageNumber(1).build(),
//                        List.of(itemB, itemC, itemA),
//                        List.of(itemA, itemB, itemC)
//                ),
//                Arguments.of(
//                        GetItemsRequestDto.builder().search(null).sort(Sort.PRICE).pageSize(3).pageNumber(1).build(),
//                        List.of(itemC, itemB, itemA),
//                        List.of(itemA, itemC, itemB)
//                ),
//                Arguments.of(
//                        GetItemsRequestDto.builder().search(null).sort(null).pageSize(2).pageNumber(1).build(),
//                        List.of(itemA, itemB, itemC),
//                        List.of(itemA, itemB)
//                ),
//                Arguments.of(
//                        GetItemsRequestDto.builder().search(null).sort(null).pageSize(2).pageNumber(2).build(),
//                        List.of(itemA, itemB, itemC),
//                        List.of(itemC)
//                ),
//                Arguments.of(
//                        GetItemsRequestDto.builder().search(null).sort(null).pageSize(2).pageNumber(3).build(),
//                        List.of(itemA, itemB, itemC),
//                        List.of()
//                ),
//                Arguments.of(
//                        GetItemsRequestDto.builder().search(null).sort(null).pageSize(5).pageNumber(1).build(),
//                        List.of(),
//                        List.of()
//                ),
//                Arguments.of(
//                        GetItemsRequestDto.builder().search(null).sort(null).build(),
//                        List.of(itemA, itemB, itemC),
//                        List.of(itemA, itemB, itemC)
//                ),
//                Arguments.of(
//                        GetItemsRequestDto.builder().search("descAlpha").sort(null).pageSize(2).pageNumber(1).build(),
//                        List.of(itemA, itemB, itemC, itemD),
//                        List.of(itemD)
//                ),
//                Arguments.of(
//                        GetItemsRequestDto.builder().search("Alpha").sort(null).pageSize(3).pageNumber(1).build(),
//                        List.of(itemA, itemD, itemC),
//                        List.of(itemA, itemD)
//                ),
//                Arguments.of(
//                        GetItemsRequestDto.builder().search(null).sort(Sort.ALPHA).pageSize(3).pageNumber(1).build(),
//                        List.of(itemB, itemA, itemC),
//                        List.of(itemA, itemB, itemC)
//                ),
//                Arguments.of(
//                        GetItemsRequestDto.builder().search(null).sort(Sort.PRICE).pageSize(3).pageNumber(1).build(),
//                        List.of(itemC, itemB, itemA),
//                        List.of(itemA, itemC, itemB)
//                ),
//                Arguments.of(
//                        GetItemsRequestDto.builder().search(null).sort(Sort.NO).pageSize(3).pageNumber(1).build(),
//                        List.of(itemC, itemB, itemA),
//                        List.of(itemC, itemB, itemA)
//                ),
//                Arguments.of(
//                        GetItemsRequestDto.builder().search("alpha").sort(null).pageSize(3).pageNumber(1).build(),
//                        List.of(itemE, itemA, itemB),
//                        List.of(itemA)
//                )
//        );
//    }
//}
