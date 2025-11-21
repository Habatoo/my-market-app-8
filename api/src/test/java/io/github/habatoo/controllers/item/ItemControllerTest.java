package io.github.habatoo.controllers.item;

import io.github.habatoo.controllers.ItemController;
import io.github.habatoo.dto.enums.Action;
import io.github.habatoo.dto.enums.Sort;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.request.GetItemsRequestDto;
import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.dto.response.ItemDtoResponse;
import io.github.habatoo.dto.response.ItemsDtoResponse;
import io.github.habatoo.servicies.CartService;
import io.github.habatoo.servicies.ItemService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Objects;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

/**
 * Unit-тесты для ItemController с использованием Mockito Extension.
 * Проверяется корректность работы методов отображения списка, изменения количества товара,
 * просмотра позиции и изменения количества из карточки товара.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для ItemController")
class ItemControllerTest {

    @Mock
    private ItemService itemService;

    @Mock
    private CartService cartService;

    @Mock
    private Model model;

    @InjectMocks
    private ItemController itemController;

    /**
     * Тест получения и отображения списка товаров с фильтрацией и пагинацией.
     * Проверяет добавление объектов в модель и возврат правильного имени шаблона.
     */
    @MethodSource("paramsCombinations")
    @DisplayName("GET /items — все варианты параметров (null, пустые, корректные)")
    void testGetItemsVariants(String search, Sort sort, Integer pageNumber, Integer pageSize) {
        GetItemsRequestDto req = GetItemsRequestDto.builder()
                .search(search)
                .sort(sort)
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .build();

        ItemsDtoResponse response = mock(ItemsDtoResponse.class);
        when(itemService.getItems(any(GetItemsRequestDto.class))).thenReturn(Mono.just(response));

        Mono<String> result = itemController.getItems(req, model);

        StepVerifier.create(result)
                .expectNext("items")
                .verifyComplete();

        verify(itemService).getItems(any(GetItemsRequestDto.class));
        verify(model).addAttribute("cart", response.cart());
        verify(model).addAttribute("items", response.itemsRows());
        verify(model).addAttribute("search", (search == null ? "" : search));
        verify(model).addAttribute("sort", sort);
        verify(model).addAttribute("paging", response.paging());
        verify(model).addAttribute("itemCounts", response.itemCounts());
    }

    /**
     * Тест изменения количества конкретного товара в корзине с редиректом на витрину и фильтрами.
     * Проверяет генерацию правильной ссылки и вызов сервиса изменения количеста.
     */
    @Test
    @DisplayName("POST /items — изменение количества товара, корректный редирект")
    void testChangeNumberOfItems() {
        ChangeNumberOfItemsRequestDto req = ChangeNumberOfItemsRequestDto.builder()
                .id(1L)
                .search("searchQuery")
                .action(Action.PLUS)
                .sort(Sort.PRICE)
                .pageNumber(2)
                .pageSize(10)
                .build();

        when(cartService.changeNumberOfItems(eq(req))).thenReturn(Mono.just(mock(ItemDto.class)));

        Mono<String> result = itemController.changeNumberOfItems(req, mock(BindingResult.class));

        StepVerifier.create(result)
                .expectNext("redirect:/items?search=searchQuery&sort=PRICE&pageSize=10&pageNumber=2")
                .verifyComplete();

        verify(cartService).changeNumberOfItems(eq(req));
    }

    /**
     * Тест отображения отдельной карточки товара и количествав корзине.
     * Проверяет передачу в модель данных товара и количества.
     */
    @Test
    @DisplayName("GET /items/{id} — отображение карточки товара")
    void testGetItemPage() {
        Long id = 42L;
        ItemDtoResponse itemResponse = mock(ItemDtoResponse.class);
        when(itemService.getItem(id)).thenReturn(Mono.just(itemResponse));

        Mono<String> result = itemController.getItemPage(id, model);

        StepVerifier.create(result)
                .expectNext("item")
                .verifyComplete();

        verify(itemService).getItem(id);
        verify(model).addAttribute("item", itemResponse.item());
        verify(model).addAttribute("cartCount", itemResponse.cartCount());
    }

    /**
     * Тест изменения количества товара из карточки товара и возврата свежей карточки.
     * Проверяет вызов метода сервиса и заполнение модели актуальными данными.
     */
    @Test
    @DisplayName("POST /items/{id} — изменение количества товара и возврат карточки")
    void testChangeItemFromItemPage() {
        Long id = 12L;
        ChangeNumberOfItemsRequestDto req = ChangeNumberOfItemsRequestDto.builder()
                .action(Action.MINUS)
                .build();
        req.setId(id);

        ItemDtoResponse itemResponse = mock(ItemDtoResponse.class);
        when(itemService.changeNumberOfItemsFromPage(eq(req))).thenReturn(Mono.just(itemResponse));

        Mono<String> result = itemController.changeItemFromItemPage(id, req, model);

        StepVerifier.create(result)
                .expectNext("item")
                .verifyComplete();

        verify(itemService).changeNumberOfItemsFromPage(eq(req));
        verify(model).addAttribute("item", itemResponse);
        verify(model).addAttribute("cartCount", itemResponse.cartCount());
    }

    @ParameterizedTest
    @MethodSource("redirectParams")
    @DisplayName("POST /changeNumberOfItems — все варианты формирования redirect")
    void changeNumberOfItemsRedirectTest(
            String search, Sort sort, Integer pageNumber, Integer pageSize,
            String expectedSearch, String expectedSort,
            int expectedPageSize, int expectedPageNumber,
            String expectedRedirect
    ) {
        ChangeNumberOfItemsRequestDto req = ChangeNumberOfItemsRequestDto.builder()
                .search(search)
                .sort(sort)
                .pageSize(pageSize)
                .pageNumber(pageNumber)
                .build();

        when(cartService.changeNumberOfItems(eq(req))).thenReturn(Mono.just(mock(ItemDto.class)));

        Mono<String> redirectMono = itemController.changeNumberOfItems(req, mock(BindingResult.class));

        StepVerifier.create(redirectMono)
                .expectNext(expectedRedirect)
                .verifyComplete();

        verify(cartService).changeNumberOfItems(argThat(requestDto ->
                Objects.equals(requestDto.getSearch(), req.getSearch()) &&
                        requestDto.getSort() == req.getSort() &&
                        Objects.equals(requestDto.getPageNumber(), req.getPageNumber()) &&
                        Objects.equals(requestDto.getPageSize(), req.getPageSize())
        ));
    }

    static Stream<Arguments> redirectParams() {
        return Stream.of(
                Arguments.of(null, null, null, null, "", "NO", 5, 1,
                        "redirect:/items?search=&sort=NO&pageSize=5&pageNumber=1"),
                Arguments.of("", Sort.NO, 1, 5, "", "NO", 5, 1,
                        "redirect:/items?search=&sort=NO&pageSize=5&pageNumber=1"),
                Arguments.of("поиск", Sort.ALPHA, 2, 10, "поиск", "ALPHA", 10, 2,
                        "redirect:/items?search=поиск&sort=ALPHA&pageSize=10&pageNumber=2"),
                Arguments.of("   ", Sort.PRICE, null, null, "   ", "PRICE", 5, 1,
                        "redirect:/items?search=   &sort=PRICE&pageSize=5&pageNumber=1"),
                Arguments.of(null, null, null, 2, "", "NO", 2, 1,
                        "redirect:/items?search=&sort=NO&pageSize=2&pageNumber=1"),
                Arguments.of(null, null, 3, null, "", "NO", 5, 3,
                        "redirect:/items?search=&sort=NO&pageSize=5&pageNumber=3"),
                Arguments.of("text", Sort.NO, null, null, "text", "NO", 5, 1,
                        "redirect:/items?search=text&sort=NO&pageSize=5&pageNumber=1")
        );
    }

    private static Stream<Arguments> paramsCombinations() {
        return Stream.of(
                Arguments.of(null, null, null, null),
                Arguments.of("", Sort.NO, 1, 5),
                Arguments.of("test", Sort.NO, 1, 5),
                Arguments.of("поиск", Sort.PRICE, 2, 10),
                Arguments.of("", Sort.ALPHA, null, null),
                Arguments.of(null, Sort.NO, 1, null),
                Arguments.of("   ", Sort.NO, null, 5),
                Arguments.of(null, null, 99, 99)
        );
    }
}
