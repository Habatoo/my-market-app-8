package io.github.habatoo.controllers.buy;

import io.github.habatoo.controllers.BuyController;
import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.dto.response.CartItemDto;
import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.servicies.BuyService;
import io.github.habatoo.servicies.CartService;
import io.github.habatoo.servicies.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
@DisplayName("Тесты unit уровня методов контроллера BuyController с использованием Cached MockMvc.")
class BuyControllerCashedTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private BuyService buyService;

    @MockitoBean
    private CartService cartService;

    /**
     * Тест успешной покупки: редирект на последнюю покупку пользователя
     */
    @Test
    @DisplayName("POST /buy — успешная покупка, редирект на последний заказ")
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

        OrderDto orderDto = mock(OrderDto.class);
        when(orderDto.id()).thenReturn(111L);
        when(orderDto.dateTime()).thenReturn(LocalDateTime.now());

        when(orderService.getOrders()).thenReturn(Flux.just(orderDto));

        doNothing().when(buyService).buy(1L);

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader()
                .valueEquals(
                        HttpHeaders.LOCATION,
                        "/orders/111?newOrder=true"
                );

        verify(buyService).buy(1L);
        verify(orderService).getOrders();
        verify(cartService).getItemsInTheCart();
    }

    /**
     * Тест для случая, когда заказов нет — редирект на базовый адрес
     */
    @Test
    @DisplayName("POST /buy — если нет заказов, редирект на базовый /orders/")
    void buyNoOrderTest() {
        CartDto cart = new CartDto(
                5L,
                List.of(),
                BigDecimal.ZERO
        );

        when(cartService.getItemsInTheCart()).thenReturn(Mono.just(cart));
        when(orderService.getOrders()).thenReturn(Flux.empty());

        doNothing().when(buyService).buy(5L);

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader()
                .valueEquals(HttpHeaders.LOCATION, "/orders/");

        verify(buyService).buy(5L);
        verify(orderService).getOrders();
        verify(cartService).getItemsInTheCart();
    }
}
