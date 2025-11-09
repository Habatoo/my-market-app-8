package io.github.habatoo.controllers.cart;

import io.github.habatoo.controllers.CartController;
import io.github.habatoo.dto.enums.Action;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.servicies.CartService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartService cartService;

    @Mock
    private Model model;

    @InjectMocks
    private CartController cartController;

    @Test
    void showCartTest() {
        CartDto cart = mock(CartDto.class);
        when(cartService.getItemsInTheCart()).thenReturn(cart);

        String result = cartController.showCart(model);

        assertEquals("cart", result);
        verify(cartService).getItemsInTheCart();
        verify(model).addAttribute("cart", cart);
    }

    @Test
    void changeNumberOfItemsFromCartTest() {
        Long id = 51L;
        String action = "PLUS";
        CartDto cart = mock(CartDto.class);

        ChangeNumberOfItemsRequestDto req =
                ChangeNumberOfItemsRequestDto.builder()
                        .id(id)
                        .action(Action.valueOf(action))
                        .build();

        when(cartService.changeNumberOfItemsFromCart(any(ChangeNumberOfItemsRequestDto.class)))
                .thenReturn(cart);

        String result = cartController.changeNumberOfItemsFromCart(id, action, model);

        assertEquals("cart", result);
        verify(cartService).changeNumberOfItemsFromCart(refEq(req));
        verify(model).addAttribute("cart", cart);
    }
}
