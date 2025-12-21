package io.github.habatoo.controllers.item;

import io.github.habatoo.controllers.ItemController;
import io.github.habatoo.dto.enums.Action;
import io.github.habatoo.dto.enums.Sort;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.request.GetItemsRequestDto;
import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.dto.response.ItemDtoResponse;
import io.github.habatoo.dto.response.ItemsDtoResponse;
import io.github.habatoo.dto.response.Paging;
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

import java.util.List;
import java.util.Map;
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

    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private ItemController itemController;

    /**
     * Тест получения и отображения списка товаров с фильтрацией и пагинацией.
     * Проверяет добавление объектов в модель и возврат правильного имени шаблона.
     */
    @ParameterizedTest
    @MethodSource("getItemsFullParameters")
    @DisplayName("GET /items — полное покрытие входных параметров и состояний ответа")
    void testGetItemsFullCoverageTest(
            String search, Sort sort, Integer pageNumber, Integer pageSize,
            boolean isAuth, int rowsCount
    ) {
        GetItemsRequestDto req = GetItemsRequestDto.builder()
                .search(search)
                .sort(sort)
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .build();

        ItemsDtoResponse response = mock(ItemsDtoResponse.class);
        List<List<ItemDto>> mockRows = Stream.generate(() -> List.of(mock(ItemDto.class)))
                .limit(rowsCount)
                .toList();

        when(response.itemsRows()).thenReturn(mockRows);
        when(response.isAuth()).thenReturn(isAuth);
        when(response.paging()).thenReturn(mock(Paging.class));
        when(response.itemCounts()).thenReturn(mock(Map.class));

        when(itemService.getItems(any(GetItemsRequestDto.class))).thenReturn(Mono.just(response));

        Mono<String> result = itemController.getItems(req, model);

        StepVerifier.create(result)
                .expectNext("items")
                .verifyComplete();

        verify(model).addAttribute("items", mockRows);
        verify(model).addAttribute("search", (search == null ? "" : search));
        verify(model).addAttribute("sort", sort);
        verify(model).addAttribute("isAuth", isAuth);
        verify(model).addAttribute("paging", response.paging());
        verify(model).addAttribute("itemCounts", response.itemCounts());

        verify(itemService).getItems(argThat(dto ->
                Objects.equals(dto.getSearch(), search) && dto.getSort() == sort
        ));
    }

    /**
     * Тест получения в данных ошибки.
     * Проверяет исключение при ошибке в данных.
     */
    @Test
    @DisplayName("GET /items — ошибка в данных")
    void changeNumberOfItemsWhenBindingResultHasErrorsTest() {
        ChangeNumberOfItemsRequestDto req = ChangeNumberOfItemsRequestDto.builder()
                .action(Action.MINUS)
                .build();

        when(bindingResult.hasErrors()).thenReturn(true);

        Mono<String> result = itemController.changeNumberOfItems(req, bindingResult);

        StepVerifier.create(result)
                .expectErrorMatches(ex ->
                        ex instanceof IllegalArgumentException
                                && ex.getMessage().equals("Некорректные параметры изменения товара")
                )
                .verify();
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
        verify(model).addAttribute("item", itemResponse.item());
        verify(model).addAttribute("cartCount", itemResponse.cartCount());
    }

    @ParameterizedTest
    @MethodSource("redirectParams")
    @DisplayName("POST /changeNumberOfItems — проверка всех комбинаций формирования redirect")
    void changeNumberOfItemsRedirectTest(
            String search, Sort sort, Integer pageNumber, Integer pageSize,
            String expectedRedirect
    ) {
        ChangeNumberOfItemsRequestDto req = ChangeNumberOfItemsRequestDto.builder()
                .search(search)
                .sort(sort)
                .pageSize(pageSize)
                .pageNumber(pageNumber)
                .build();

        BindingResult binding = mock(BindingResult.class);

        when(cartService.changeNumberOfItems(any(ChangeNumberOfItemsRequestDto.class)))
                .thenReturn(Mono.just(mock(ItemDto.class)));

        Mono<String> redirectMono = itemController.changeNumberOfItems(req, binding);

        StepVerifier.create(redirectMono)
                .expectNext(expectedRedirect)
                .verifyComplete();

        verify(cartService).changeNumberOfItems(argThat(dto ->
                Objects.equals(dto.getSearch(), search) &&
                        dto.getSort() == sort &&
                        Objects.equals(dto.getPageNumber(), pageNumber) &&
                        Objects.equals(dto.getPageSize(), pageSize)
        ));
    }

    private static Stream<Arguments> redirectParams() {
        return Stream.of(
                Arguments.of(null, null, null, null,
                        "redirect:/items?search=&sort=NO&pageSize=5&pageNumber=1"),
                Arguments.of("phone", null, null, null,
                        "redirect:/items?search=phone&sort=NO&pageSize=5&pageNumber=1"),
                Arguments.of(null, Sort.PRICE, null, null,
                        "redirect:/items?search=&sort=PRICE&pageSize=5&pageNumber=1"),
                Arguments.of(null, null, 3, null,
                        "redirect:/items?search=&sort=NO&pageSize=5&pageNumber=3"),
                Arguments.of(null, null, null, 20,
                        "redirect:/items?search=&sort=NO&pageSize=20&pageNumber=1"),
                Arguments.of("text", Sort.ALPHA, null, null,
                        "redirect:/items?search=text&sort=ALPHA&pageSize=5&pageNumber=1"),
                Arguments.of("book", null, 2, null,
                        "redirect:/items?search=book&sort=NO&pageSize=5&pageNumber=2"),
                Arguments.of("note", null, null, 9,
                        "redirect:/items?search=note&sort=NO&pageSize=9&pageNumber=1"),
                Arguments.of(null, Sort.PRICE, 2, 50,
                        "redirect:/items?search=&sort=PRICE&pageSize=50&pageNumber=2"),
                Arguments.of("laptop", Sort.ALPHA, 1, 10,
                        "redirect:/items?search=laptop&sort=ALPHA&pageSize=10&pageNumber=1"),
                Arguments.of("", Sort.NO, 5, 15,
                        "redirect:/items?search=&sort=NO&pageSize=15&pageNumber=5"),
                Arguments.of("   ", Sort.PRICE, null, null,
                        "redirect:/items?search=   &sort=PRICE&pageSize=5&pageNumber=1")
        );
    }

    /**
     * Расширенный источник данных, объединяющий старые комбинации параметров
     * с новыми условиями (авторизация и наполнение данными).
     * * Параметры: search, sort, pageNumber, pageSize, isAuth, rowsCount
     */
    private static Stream<Arguments> getItemsFullParameters() {
        return Stream.of(
                Arguments.of(null, null, null, null, false, 0),
                Arguments.of("", Sort.NO, 1, 5, true, 5),
                Arguments.of("test", Sort.NO, 1, 5, false, 2),
                Arguments.of("поиск", Sort.PRICE, 2, 10, true, 10),
                Arguments.of("", Sort.ALPHA, null, null, false, 0),
                Arguments.of(null, Sort.NO, 1, null, true, 3),
                Arguments.of("   ", Sort.NO, null, 5, false, 1),
                Arguments.of(null, null, 99, 99, true, 0),
                Arguments.of("special-char-!@#", Sort.PRICE, 1, 5, true, 1),
                Arguments.of("long".repeat(10), Sort.NO, 1, 10, false, 0),
                Arguments.of(null, Sort.ALPHA, -1, -5, true, 5),
                Arguments.of("only-auth-user-items", null, 1, 10, true, 15),
                Arguments.of("no-items-found", Sort.NO, 1, 10, false, 0)
        );
    }
}
