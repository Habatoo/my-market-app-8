package io.github.habatoo.mappers;

import io.github.habatoo.dto.response.OrderItemDto;
import io.github.habatoo.entity.Item;
import io.github.habatoo.entity.OrderItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Интеграционный тест OrderItemMapper — маппер")
class OrderItemMapperIntegrationTest {
    @Autowired
    private OrderItemMapper orderItemMapper;

    @Test
    @DisplayName("Маппинг OrderItem в DTO: обычный кейс")
    void mapOrderItemToDtoTest() {
        Item item = new Item();
        item.setId(7L);

        OrderItem entity = new OrderItem();
        entity.setId(17L);
        entity.setItem(item);
        entity.setPrice(BigDecimal.valueOf(100));
        entity.setCount(3);

        OrderItemDto dto = orderItemMapper.toDto(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.item().id()).isEqualTo(7L);
        assertThat(dto.price()).isEqualTo(BigDecimal.valueOf(100));
        assertThat(dto.count()).isEqualTo(3);
        assertThat(dto.total()).isEqualTo(BigDecimal.valueOf(300)); // 100*3
    }

    @Test
    @DisplayName("Маппинг OrderItem в DTO: price или count null — total=0")
    void mapOrderItemNullPriceOrCountTest() {
        OrderItem entity = new OrderItem();
        entity.setId(41L);
        entity.setPrice(null);
        entity.setCount(null);

        OrderItemDto dto = orderItemMapper.toDto(entity);

        assertThat(dto.total()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Маппинг списка OrderItem в список DTO")
    void mapOrderItemListTest() {
        OrderItem entityA = new OrderItem();
        entityA.setId(1L);
        entityA.setPrice(BigDecimal.valueOf(10));
        entityA.setCount(2);

        OrderItem entityB = new OrderItem();
        entityB.setId(2L);
        entityB.setPrice(BigDecimal.valueOf(7));
        entityB.setCount(4);

        List<OrderItemDto> dtos = orderItemMapper.toDto(List.of(entityA, entityB));
        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).total()).isEqualTo(BigDecimal.valueOf(20));
        assertThat(dtos.get(1).total()).isEqualTo(BigDecimal.valueOf(28));
    }

    @Test
    @DisplayName("Маппинг пустого списка OrderItem")
    void mapEmptyOrderItemListTest() {
        assertThat(orderItemMapper.toDto(List.of())).isEmpty();
    }
}
