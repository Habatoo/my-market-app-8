package io.github.habatoo.mappers;

import io.github.habatoo.dto.response.OrderItemDto;
import io.github.habatoo.entity.Item;
import io.github.habatoo.entity.OrderItem;
import io.github.habatoo.utils.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Интеграционный тест OrderItemMapper — маппер")
class OrderItemMapperIntegrationTest extends BaseTest {

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Test
    @DisplayName("Маппинг OrderItem в DTO: обычный кейс")
    void mapOrderItemToDtoTest() {
        Item item = new Item();
        item.setId(7L);

        OrderItem entity = new OrderItem();
        entity.setId(17L);
        entity.setItemId(7L);
        entity.setOrderId(10L);
        entity.setCount(3);
        entity.setPrice(BigDecimal.valueOf(100));

        OrderItemDto dto = orderItemMapper.toDto(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.count()).isEqualTo(3);
        assertThat(dto.price()).isEqualTo(BigDecimal.valueOf(100));
        assertThat(dto.total()).isEqualTo(BigDecimal.valueOf(300));
    }

    @Test
    @DisplayName("Маппинг пустого списка OrderItem")
    void mapEmptyOrderItemListTest() {
        assertThat(orderItemMapper.toDto(null)).isNull();
    }
}
