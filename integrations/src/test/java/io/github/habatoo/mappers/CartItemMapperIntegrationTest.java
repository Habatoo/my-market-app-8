package io.github.habatoo.mappers;

import io.github.habatoo.dto.response.CartItemDto;
import io.github.habatoo.entity.CartItem;
import io.github.habatoo.utils.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Reactive интеграционный тест CartItemMapper — маппер")
class CartItemMapperIntegrationTest extends BaseTest {

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private CartItemMapper cartItemMapper;

    @Test
    @DisplayName("Маппинг одного CartItem в DTO (с реактивной подготовкой)")
    void mapSingleCartItemTest() {
        Mono<CartItem> prepared = createAndSaveCart()
                .flatMap(cart -> createAndSaveItem("Товар", BigDecimal.valueOf(99))
                        .flatMap(item -> createAndSaveCartItem(cart, item, 2, item.getPrice()))
                );

        StepVerifier.create(prepared)
                .assertNext(cartItem -> {
                    CartItemDto dto = cartItemMapper.toDto(cartItem);
                    assertThat(dto).isNotNull();
                    assertThat(dto.count()).isEqualTo(2);
                    assertThat(dto.price()).isEqualTo(BigDecimal.valueOf(99));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Маппинг пустого списка CartItem")
    void mapEmptyCartItemListTest() {
        CartItemDto dtos = cartItemMapper.toDto(null);
        assertThat(dtos == null);
    }
}
