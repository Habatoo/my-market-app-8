package io.github.habatoo.controllers.item;

import io.github.habatoo.configurations.DisableViewResolverConfiguration;
import io.github.habatoo.controllers.ItemController;
import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.dto.response.ItemDtoResponse;
import io.github.habatoo.dto.response.ItemsDtoResponse;
import io.github.habatoo.handlers.GlobalExceptionHandler;
import io.github.habatoo.servicies.CartService;
import io.github.habatoo.servicies.ItemService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для ItemController.
 * Проверяет обработку фильтрации, пагинации, изменение количества товара в корзине и отображение позиции товара.
 * Использует WebFluxTest для имитации HTTP-запросов и проверки модели/шаблонов.
 */
@WebFluxTest(ItemController.class)
@ContextConfiguration(classes = ItemController.class)
@Import({DisableViewResolverConfiguration.class, GlobalExceptionHandler.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Тесты unit-уровня методов ItemController с использованием WebFluxTest")
class ItemControllerCashedTest {

    private static final String ITEMS = "items";
    private static final String ITEM = "item";

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private CartService cartService;

    /**
     * Тест отображения витрины товаров с фильтрацией/пагинацией и корзиной.
     * Проверяет возврат шаблона и атрибутов модели (товары, корзина, поисковые параметры).
     */
    @Test
    @DisplayName("GET \"/items\" — отображение витрины с параметрами поиска и пагинации")
    void getItemsTest() {
        ItemsDtoResponse itemsDtoResponse = ItemsDtoResponse.builder()
                .itemsRows(List.of(List.of()))
                .cart(mock(CartDto.class))
                .build();

        when(itemService.getItems(any())).thenReturn(Mono.just(itemsDtoResponse));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("search", "test")
                        .queryParam("sort", "NO")
                        .queryParam("pageSize", "5")
                        .queryParam("pageNumber", "1")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String view = result.getResponseBody();
                    assertEquals(ITEMS, view);
                });

        verify(itemService).getItems(any());
    }

    /**
     * Тест обработки изменения количества товара в корзине и редиректа на витрину.
     * Проверяется вызов сервиса и корректный редирект с сохранением фильтров.
     */
    @Test
    @DisplayName("POST \"/items\" — изменение количества товара и редирект с фильтрами")
    void changeNumberOfItemsTest() {
        when(cartService.changeNumberOfItems(any())).thenReturn(Mono.just(mock(ItemDto.class)));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("id", "15")
                        .queryParam("action", "PLUS")
                        .queryParam("search", "test")
                        .queryParam("sort", "NO")
                        .queryParam("pageSize", "5")
                        .queryParam("pageNumber", "1")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals(
                        "Location",
                        "/items?search=test&sort=NO&pageSize=5&pageNumber=1");

        verify(cartService).changeNumberOfItems(any());
    }

    /**
     * Тест отображения отдельной позиции товара и количества в корзине.
     * Проверяет правильную передачу модели и ожидание нужного view.
     */
    @Test
    @DisplayName("GET \"/items/{id}\" — отображение карточки позиции товара")
    void getItemPageTest() {
        ItemDtoResponse itemDtoResponse = ItemDtoResponse.builder()
                .cartCount(3)
                .build();

        when(itemService.getItem(anyLong())).thenReturn(Mono.just(itemDtoResponse));

        webTestClient.get()
                .uri("/items/33")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String view = result.getResponseBody();
                    assertEquals(ITEM, view);
                });

        verify(itemService).getItem(33L);
    }

    /**
     * Тест изменения количества товара из страницы позиции и возврата этой же страницы.
     * Проверяет передачу модели и вызов соответствующего метода сервиса.
     */
    @Test
    @DisplayName("POST \"/items/{id}\" — изменение количества товара на странице и возвращение позиции")
    void changeItemFromItemPageTest() {
        ItemDtoResponse itemDtoResponse = mock(ItemDtoResponse.class);
        when(itemService.changeNumberOfItemsFromPage(any())).thenReturn(Mono.just(itemDtoResponse));

        webTestClient.post()
                .uri("/items/33")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String view = result.getResponseBody();
                    assertEquals(ITEM, view);
                });

        verify(itemService).changeNumberOfItemsFromPage(any());
    }

    /**
     * Тест товар не найден.
     * Проверяет передачу модели и вызов GlobalExceptionHandler.
     */
    @Test
    @DisplayName("GET /items/{id} — товар не найден → GlobalExceptionHandler 404")
    void getItemNotFoundTest() {
        when(itemService.getItem(anyLong())).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/items/999")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String view = result.getResponseBody();
                    assertEquals(ITEM, view);
                });

        verify(itemService).getItem(999L);
    }
}
