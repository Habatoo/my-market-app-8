//package io.github.habatoo.mappers;
//
//import io.github.habatoo.dto.response.CartItemDto;
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
//@DisplayName("Интеграционный тест CartItemMapper — маппер")
//class CartItemMapperIntegrationTest {
//
//    @Autowired
//    private ItemMapper itemMapper;
//
//    @Autowired
//    private CartItemMapper cartItemMapper;
//
//    @Test
//    @DisplayName("Маппинг одного CartItem в DTO")
//    void mapSingleCartItemTest() {
//        Item item = new Item();
//        item.setId(10L);
//        item.setTitle("Товар");
//        item.setPrice(BigDecimal.valueOf(99));
//
//        CartItem cartItem = new CartItem();
//        cartItem.setItem(item);
//        cartItem.setCount(2);
//        cartItem.setPrice(item.getPrice());
//
//        CartItemDto dto = cartItemMapper.toDto(cartItem);
//
//        assertThat(dto).isNotNull();
//        assertThat(dto.item()).isNotNull();
//        assertThat(dto.item().id()).isEqualTo(item.getId());
//        assertThat(dto.count()).isEqualTo(2);
//        assertThat(dto.price()).isEqualTo(BigDecimal.valueOf(99));
//    }
//
//    @Test
//    @DisplayName("Маппинг списка CartItem в список DTO")
//    void mapCartItemListTest() {
//        Item itemA = new Item();
//        itemA.setId(1L);
//        itemA.setTitle("A");
//        itemA.setPrice(BigDecimal.valueOf(10));
//        CartItem cartItemA = new CartItem();
//        cartItemA.setItem(itemA);
//        cartItemA.setCount(1);
//        cartItemA.setPrice(itemA.getPrice());
//
//        Item itemB = new Item();
//        itemB.setId(2L);
//        itemB.setTitle("B");
//        itemB.setPrice(BigDecimal.valueOf(20));
//        CartItem cartItemB = new CartItem();
//        cartItemB.setItem(itemB);
//        cartItemB.setCount(3);
//        cartItemB.setPrice(itemB.getPrice());
//
//        List<CartItemDto> dtos = cartItemMapper.toDto(List.of(cartItemA, cartItemB));
//
//        assertThat(dtos).hasSize(2);
//        assertThat(dtos.get(0).item().id()).isEqualTo(1L);
//        assertThat(dtos.get(0).count()).isEqualTo(1);
//        assertThat(dtos.get(1).item().id()).isEqualTo(2L);
//        assertThat(dtos.get(1).count()).isEqualTo(3);
//    }
//
//    @Test
//    @DisplayName("Маппинг пустого списка CartItem")
//    void mapEmptyCartItemListTest() {
//        List<CartItemDto> dtos = cartItemMapper.toDto(List.of());
//        assertThat(dtos).isEmpty();
//    }
//}
