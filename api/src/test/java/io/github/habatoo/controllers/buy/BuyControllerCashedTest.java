package io.github.habatoo.controllers.buy;

import io.github.habatoo.controllers.BuyController;
import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.dto.response.CartItemDto;
import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.servicies.BuyService;
import io.github.habatoo.servicies.CartService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * <h2>Тесты для BuyController c максимальным кешированием WebFluxTest</h2>
 *
 * <p>
 * Класс покрывает unit-тесты основных методов контроллера BuyController с использованием WebFluxTest.
 * Тесты полностью изолированы от Spring-контекста — мокируется только сервисный слой CommentService.
 * </p>
 */
@WebFluxTest(BuyController.class)
@ContextConfiguration(classes = BuyController.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Тесты BuyController с использованием WebTestClient")
class BuyControllerCashedTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private BuyService buyService;

    @MockitoBean
    private CartService cartService;

    /**
     * Тест успешной покупки: редирект на созданный заказ
     */
    @Test
    @DisplayName("POST /buy — успешная покупка, редирект на созданный заказ")
    void buySuccessTest() {
        CartDto cart = new CartDto(
                1L,
                List.of(
                        new CartItemDto(
                                new ItemDto(
                                        15L,
                                        "title",
                                        "desc",
                                        "img/path",
                                        BigDecimal.TEN,
                                        1
                                ),
                                1,
                                BigDecimal.TEN
                        )
                ),
                BigDecimal.TEN
        );

        when(cartService.getItemsInTheCart()).thenReturn(Mono.just(cart));
        when(buyService.buy(1L)).thenReturn(Mono.just(111L));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader()
                .valueEquals(HttpHeaders.LOCATION, "/orders/111?newOrder=true");

        verify(cartService).getItemsInTheCart();
        verify(buyService).buy(1L);
    }

    /**
     * Тест, когда корзина есть, но заказ не создается (пустой Mono) — редирект на /orders/
     */
    @Test
    @DisplayName("POST /buy — корзина есть, но заказ не создается, редирект на /orders/")
    void buyNoOrderTest() {
        CartDto cart = new CartDto(
                5L,
                List.of(),
                BigDecimal.ZERO
        );

        when(cartService.getItemsInTheCart()).thenReturn(Mono.just(cart));
        when(buyService.buy(5L)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader()
                .valueEquals(HttpHeaders.LOCATION, "/orders/");

        verify(cartService).getItemsInTheCart();
        verify(buyService).buy(5L);
    }

    /**
     * Тест, когда корзина пуста — редирект на /orders/
     */
    @Test
    @DisplayName("POST /buy — корзина пуста, редирект на /orders/")
    void buyEmptyCartTest() {
        when(cartService.getItemsInTheCart()).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader()
                .valueEquals(HttpHeaders.LOCATION, "/orders/");

        verify(cartService).getItemsInTheCart();
        verifyNoInteractions(buyService);
    }
}
