package io.github.habatoo.controllers.buy;

import io.github.habatoo.controllers.BuyController;
import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.servicies.BuyService;
import io.github.habatoo.servicies.CartService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

/**
 * Unit-тесты для BuyController с использованием Mockito.
 * Проверяет обработку сценариев успешной покупки (создаётся новый заказ) и покупки без заказов.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты для BuyController")
class BuyControllerTest {

    @Mock
    private CartService cartService;

    @Mock
    private BuyService buyService;

    @InjectMocks
    private BuyController buyController;

    /**
     * Тест: успешная покупка с возвратом редиректа на созданный заказ
     */
    @Test
    @DisplayName("POST /buy — успешная покупка возвращает redirect на новый заказ")
    void testBuySuccess() {
        CartDto cartDto = new CartDto(1L, null, null);
        when(cartService.getItemsInTheCart()).thenReturn(Mono.just(cartDto));

        when(buyService.buy(1L)).thenReturn(Mono.just(42L));

        Mono<String> result = buyController.buy();

        StepVerifier.create(result)
                .expectNext("redirect:/orders/42?newOrder=true")
                .verifyComplete();

        verify(cartService).getItemsInTheCart();
        verify(buyService).buy(1L);
    }

    /**
     * Тест: корзина пуста — возвращаем просто редирект на список заказов
     */
    @Test
    @DisplayName("POST /buy — корзина пустая возвращает redirect на ORDERS")
    void testBuyEmptyCart() {
        when(cartService.getItemsInTheCart()).thenReturn(Mono.empty());

        Mono<String> result = buyController.buy();

        StepVerifier.create(result)
                .expectNext("redirect:/orders/")
                .verifyComplete();

        verify(cartService).getItemsInTheCart();
        verifyNoInteractions(buyService);
    }

    /**
     * Тест: корзина есть, но buy возвращает пустой Mono — возвращаем просто редирект на ORDERS
     */
    @Test
    @DisplayName("POST /buy — корзина есть, но покупка не создается, возвращаем редирект на ORDERS")
    void testBuyNoOrderCreated() {
        CartDto cartDto = new CartDto(1L, null, null);
        when(cartService.getItemsInTheCart()).thenReturn(Mono.just(cartDto));

        when(buyService.buy(1L)).thenReturn(Mono.empty());

        Mono<String> result = buyController.buy();

        StepVerifier.create(result)
                .expectNext("redirect:/orders/")
                .verifyComplete();

        verify(cartService).getItemsInTheCart();
        verify(buyService).buy(1L);
    }

    /**
     * Тест: buy бросает исключение — контроллер пробрасывает ошибку
     */
    @Test
    @DisplayName("POST /buy — ошибка в сервисе покупки пробрасывается")
    void testBuyError() {
        CartDto cartDto = new CartDto(1L, null, null);
        when(cartService.getItemsInTheCart()).thenReturn(Mono.just(cartDto));
        when(buyService.buy(1L)).thenReturn(Mono.error(new RuntimeException("Ошибка покупки")));

        Mono<String> result = buyController.buy();

        StepVerifier.create(result)
                .expectErrorMessage("Ошибка покупки")
                .verify();

        verify(cartService).getItemsInTheCart();
        verify(buyService).buy(1L);
    }
}
