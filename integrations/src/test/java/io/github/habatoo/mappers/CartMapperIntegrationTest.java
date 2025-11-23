//package io.github.habatoo.mappers;
//
//import io.github.habatoo.dto.response.CartDto;
//import io.github.habatoo.dto.response.CartItemDto;
//import io.github.habatoo.entity.Cart;
//import io.github.habatoo.entity.CartItem;
//import io.github.habatoo.entity.Item;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.math.BigDecimal;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@SpringBootTest
//@DisplayName("Интеграционный тест CartMapper — маппер")
//class CartMapperIntegrationTest {
//    @Autowired
//    private CartMapper cartMapper;
//
//    @Test
//    @DisplayName("Маппинг Cart в CartDto с позиции CartItem")
//    void mapCartToDtoTest() {
//        Item item = new Item();
//        item.setId(21L);
//        item.setTitle("Монитор");
//        item.setPrice(BigDecimal.valueOf(200));
//
//        CartItem cartItem = new CartItem();
//        cartItem.setItem(item);
//        cartItem.setCount(2);
//        cartItem.setPrice(item.getPrice());
//
//        Cart cart = new Cart();
//        cart.setId(99L);
//        cart.setItems(List.of(cartItem));
//        cart.setTotal(BigDecimal.valueOf(400));
//
//        CartDto dto = cartMapper.toDto(cart);
//
//        assertThat(dto).isNotNull();
//        assertThat(dto.id()).isEqualTo(99L);
//        assertThat(dto.total()).isEqualTo(BigDecimal.valueOf(400));
//        assertThat(dto.items()).hasSize(1);
//
//        CartItemDto dtoItem = dto.items().get(0);
//        assertThat(dtoItem.count()).isEqualTo(2);
//        assertThat(dtoItem.price()).isEqualTo(BigDecimal.valueOf(200));
//        assertThat(dtoItem.item().title()).isEqualTo("Монитор");
//    }
//
//    @Test
//    @DisplayName("Маппинг списка Cart в список CartDto")
//    void mapListCartToDtoTest() {
//        Item itemA = new Item();
//        itemA.setId(1L);
//        itemA.setTitle("A");
//        itemA.setPrice(BigDecimal.valueOf(50));
//        CartItem cartItemA = new CartItem();
//        cartItemA.setItem(itemA);
//        cartItemA.setCount(1);
//        cartItemA.setPrice(itemA.getPrice());
//        Cart cartA = new Cart();
//        cartA.setId(1L);
//        cartA.setItems(List.of(cartItemA));
//        cartA.setTotal(BigDecimal.valueOf(50));
//
//        Item itemB = new Item();
//        itemB.setId(2L);
//        itemB.setTitle("B");
//        itemB.setPrice(BigDecimal.valueOf(60));
//        CartItem cartItemB = new CartItem();
//        cartItemB.setItem(itemB);
//        cartItemB.setCount(2);
//        cartItemB.setPrice(itemB.getPrice());
//        Cart cartB = new Cart();
//        cartB.setId(2L);
//        cartB.setItems(List.of(cartItemB));
//        cartB.setTotal(BigDecimal.valueOf(120));
//
//        List<CartDto> dtos = cartMapper.toDto(List.of(cartA, cartB));
//
//        assertThat(dtos).hasSize(2);
//        assertThat(dtos.get(0).id()).isEqualTo(1L);
//        assertThat(dtos.get(1).id()).isEqualTo(2L);
//        assertThat(dtos.get(0).items().get(0).item().title()).isEqualTo("A");
//        assertThat(dtos.get(1).items().get(0).item().title()).isEqualTo("B");
//    }
//
//    @Test
//    @DisplayName("Маппинг пустого списка Cart = пустой список CartDto")
//    void mapEmptyCartListTest() {
//        assertThat(cartMapper.toDto(List.of())).isEmpty();
//    }
//}
