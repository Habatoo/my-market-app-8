package io.github.habatoo.controllers;

import io.github.habatoo.dto.enums.Sort;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.request.GetItemsRequestDto;
import io.github.habatoo.dto.response.*;
import io.github.habatoo.handlers.GlobalExceptionHandler;
import io.github.habatoo.servicies.CartService;
import io.github.habatoo.servicies.ItemService;
import io.github.habatoo.utils.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockOidcLogin;

/**
 * Интеграционные тесты для ItemController с использованием @SpringBootTest.
 * Покрыты сценарии: получение витрины товаров с фильтрами, успешное изменение количества (POST),
 * возврат отдельной карточки, изменение количества из карточки, ошибки сервисов.
 */
@AutoConfigureWebTestClient
@Import(GlobalExceptionHandler.class)
@ImportAutoConfiguration(ThymeleafAutoConfiguration.class)
@DisplayName("Интеграционный SpringBootTest тест ItemController")
class ItemControllerIntegrationTest extends BaseTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private CartService cartService;

    /**
     * Получение витрины товаров с параметрами фильтрации.
     */
    @Test
    @DisplayName("GET /items — витрина товаров с фильтрами, корректный model и view")
    void getItemsListTest() {
        GetItemsRequestDto req = GetItemsRequestDto.builder()
                .search("test")
                .sort(Sort.ALPHA)
                .pageSize(10)
                .pageNumber(2)
                .build();

        Map<Long, Integer> itemCounts = Map.of(1L, 1);
        ItemsDtoResponse itemsDto = ItemsDtoResponse.builder()
                .itemsRows(List.of(List.of(new ItemDto(
                        1L, "A", "desc", "", BigDecimal.ONE, 1))))
                .itemCounts(itemCounts)
                .cart(mock(CartDto.class))
                .paging(mock(Paging.class))
                .build();

        when(itemService.getItems(any(GetItemsRequestDto.class)))
                .thenReturn(Mono.just(itemsDto));

        webTestClient
                .mutateWith(mockOidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .mutateWith(csrf())
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("search", "test")
                        .queryParam("sort", "ALPHA")
                        .queryParam("pageSize", "10")
                        .queryParam("pageNumber", "2")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(res -> {
                    String html = res.getResponseBody();
                    assertNotNull(html);
                    assertTrue(html.contains("items"));
                });
    }

    /**
     * Изменение количества товара — редирект на витрину с сохранением фильтров.
     */
    @Test
    @DisplayName("POST /items — изменение количества товара, редирект на витрину с фильтрами")
    void changeNumberOfItemsRedirectTest() {
        when(cartService.changeNumberOfItems(any(ChangeNumberOfItemsRequestDto.class)))
                .thenReturn(Mono.just(mock(ItemDto.class)));

        webTestClient
                .mutateWith(mockOidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .mutateWith(csrf())
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("id", "5")
                        .queryParam("action", "PLUS")
                        .queryParam("search", "abc")
                        .queryParam("sort", "PRICE")
                        .queryParam("pageSize", "7")
                        .queryParam("pageNumber", "2")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals(
                        "Location",
                        "/items?search=abc&sort=PRICE&pageSize=7&pageNumber=2"
                );
    }

    /**
     * Получение отдельной карточки товара.
     */
    @Test
    @DisplayName("GET /items/{id} — возврат отдельной карточки товара")
    void getItemPageSuccessTest() {
        ItemDtoResponse resp = new ItemDtoResponse(
                mock(ItemDto.class),
                33,
                true
        );

        when(itemService.getItem(77L)).thenReturn(Mono.just(resp));

        webTestClient
                .mutateWith(mockOidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .mutateWith(csrf())
                .get()
                .uri("/items/77")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(res -> {
                    String html = res.getResponseBody();
                    assertNotNull(html);
                    assertTrue(html.contains("item"));
                    assertTrue(html.contains("33"));
                });
    }

    /**
     * Изменение количества из страницы карточки.
     */
    @Test
    @DisplayName("POST /items/{id} — изменение количества из карточки, возврат её же")
    void changeItemFromItemPageTest() {
        ItemDtoResponse resp = new ItemDtoResponse(
                new ItemDto(1L, "title", "desc", "img/path", BigDecimal.ONE, 1),
                17,
                true
        );

        when(itemService.changeNumberOfItemsFromPage(any(ChangeNumberOfItemsRequestDto.class)))
                .thenReturn(Mono.just(resp));

        webTestClient
                .mutateWith(mockOidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .mutateWith(csrf())
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items/88")
                        .queryParam("action", "PLUS")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(res -> {
                    String html = res.getResponseBody();
                    assertNotNull(html);
                    assertTrue(html.contains("item"));
                    assertTrue(html.contains("17"));
                });
    }

    /**
     * Ошибка при получении отдельного товара (IllegalStateException).
     */
    @Test
    @DisplayName("GET /items/{id} — ошибка сервиса, отображается error/500")
    void getItemPageErrorTest() {
        when(itemService.getItem(404L))
                .thenReturn(Mono.error(new IllegalStateException("Товар не найден")));

        webTestClient
                .mutateWith(mockOidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .mutateWith(csrf())
                .get()
                .uri("/items/404")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String view = result.getResponseBody();
                    assertNotNull(view);
                    assertTrue(view.contains("Ошибка 500 — Внутренняя ошибка сервера"));
                    assertTrue(view.contains("Товар не найден"));
                });
    }

    @Test
    @DisplayName("POST /items — анонимный пользователь, обработка Access Denied")
    void changeNumberOfItemsUnauthorizedTest() {
        webTestClient
                .mutateWith(csrf())
                .post()
                .uri("/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assertNotNull(body);
                    assertTrue(body.contains("Ошибка 500") || body.contains("Access Denied"));
                });

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST /items/{id} — анонимный пользователь, обработка Access Denied")
    void changeItemFromItemPageUnauthorizedTest() {
        webTestClient
                .mutateWith(csrf())
                .post()
                .uri("/items/88")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assertNotNull(body);
                    assertTrue(body.contains("Ошибка 500"));
                });

        verifyNoInteractions(itemService);
    }

    @Test
    @DisplayName("GET /items — анонимный пользователь, доступ разрешен")
    void getItemsListAnonymousTest() {
        ItemsDtoResponse itemsDto = ItemsDtoResponse.builder()
                .itemsRows(List.of())
                .itemCounts(Map.of())
                .isAuth(false)
                .build();

        when(itemService.getItems(any(GetItemsRequestDto.class)))
                .thenReturn(Mono.just(itemsDto));

        webTestClient
                .get()
                .uri("/items")
                .exchange()
                .expectStatus().isOk();
    }
}
