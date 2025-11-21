package io.github.habatoo.controllers.buy;

import io.github.habatoo.controllers.BuyController;
import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.servicies.BuyService;
import io.github.habatoo.servicies.CartService;
import io.github.habatoo.servicies.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Unit-тесты для BuyController с использованием Mockito.
 * Проверяет обработку сценариев успешной покупки (создаётся новый заказ) и покупки без заказов.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для BuyController")
class BuyControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private CartService cartService;

    @Mock
    private BuyService buyService;

    @InjectMocks
    private BuyController buyController;

    /**
     * Тест кейс: успешное оформление покупки — должен произойти редирект на последний заказ с флагом newOrder.
     */
    @Test
    @DisplayName("Успешная покупка — редирект на новый заказ с флагом newOrder")
    void testBuyWithNewOrder() {
        CartDto cart = CartDto.builder().id(1L).build();

        LocalDateTime t1 = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime t2 = LocalDateTime.of(2024, 1, 1, 10, 5);

        OrderDto order1 = OrderDto.builder().id(10L).dateTime(t1).build();
        OrderDto order2 = OrderDto.builder().id(11L).dateTime(t2).build();

        when(cartService.getItemsInTheCart()).thenReturn(Mono.just(cart));
        doNothing().when(buyService).buy(1L);
        when(orderService.getOrders()).thenReturn(Flux.just(order1, order2));

        Mono<String> result = buyController.buy();

        StepVerifier.create(result)
                .expectNext("redirect:/orders/11?newOrder=true")
                .verifyComplete();

        verify(buyService).buy(1L);
        verify(orderService).getOrders();
        verify(cartService).getItemsInTheCart();
    }

    /**
     * Тест кейс: корзина куплена, но новых заказов нет — должен произойти редирект на страницу заказов.
     */
    @Test
    @DisplayName("Покупка без заказов — редирект на список заказов")
    void testBuyNoOrders() {
        CartDto cart = CartDto.builder().id(2L).build();

        when(cartService.getItemsInTheCart()).thenReturn(Mono.just(cart));
        doNothing().when(buyService).buy(2L);
        when(orderService.getOrders()).thenReturn(Flux.fromIterable(List.of()));

        Mono<String> result = buyController.buy();

        StepVerifier.create(result)
                .expectNext("redirect:/orders/")
                .verifyComplete();

        verify(buyService).buy(2L);
        verify(orderService).getOrders();
        verify(cartService).getItemsInTheCart();
    }
}
