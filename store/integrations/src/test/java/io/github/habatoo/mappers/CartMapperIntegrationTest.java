package io.github.habatoo.mappers;

import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.entity.Cart;
import io.github.habatoo.utils.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Reactive интеграционный тест CartMapper — маппер")
class CartMapperIntegrationTest extends BaseTest {

    @Autowired
    private CartMapper cartMapper;

    @Test
    @DisplayName("Маппинг одного Cart в CartDto")
    void mapSingleCartToDtoTest() {
        Mono<Cart> prepared = createAndSaveCart(BigDecimal.valueOf(400));

        StepVerifier.create(prepared)
                .assertNext(cart -> {
                    CartDto dto = cartMapper.toDto(cart);

                    assertThat(dto).isNotNull();
                    assertThat(dto.id()).isEqualTo(cart.getId());
                    assertThat(dto.total()).isEqualTo(cart.getTotal());
                    assertThat(dto.items()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Маппинг списка Cart в список CartDto")
    void mapListCartToDtoTest() {
        Mono<List<Cart>> prepared = Mono.zip(
                        createAndSaveCart(BigDecimal.valueOf(50)),
                        createAndSaveCart(BigDecimal.valueOf(120))
                )
                .map(tuple -> List.of(tuple.getT1(), tuple.getT2()));

        StepVerifier.create(prepared)
                .assertNext(carts -> {
                    List<CartDto> dtos = cartMapper.toDto(carts);

                    assertThat(dtos).hasSize(2);
                    assertThat(dtos.get(0).id()).isEqualTo(carts.get(0).getId());
                    assertThat(dtos.get(0).total()).isEqualTo(BigDecimal.valueOf(50));
                    assertThat(dtos.get(0).items()).isEmpty();

                    assertThat(dtos.get(1).id()).isEqualTo(carts.get(1).getId());
                    assertThat(dtos.get(1).total()).isEqualTo(BigDecimal.valueOf(120));
                    assertThat(dtos.get(1).items()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Маппинг пустого списка Cart = пустой список CartDto")
    void mapEmptyCartListTest() {
        List<CartDto> dtos = cartMapper.toDto(List.of());
        assertThat(dtos).isEmpty();
    }
}
