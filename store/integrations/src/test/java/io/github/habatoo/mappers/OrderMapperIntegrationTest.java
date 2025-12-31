package io.github.habatoo.mappers;

import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.entity.Order;
import io.github.habatoo.entity.OrderItem;
import io.github.habatoo.utils.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Интеграционный тест OrderMapper — маппер")
class OrderMapperIntegrationTest extends BaseTest {

    @Autowired
    private OrderMapper orderMapper;

    @Test
    @DisplayName("Маппинг списка Order в список OrderDto")
    void mapOrderListTest() {
        OrderItem itemA = new OrderItem();
        itemA.setId(1L);
        itemA.setPrice(BigDecimal.valueOf(10));
        itemA.setCount(1);
        Order orderA = new Order();
        orderA.setId(1L);
        orderA.setTotalSum(BigDecimal.valueOf(10));
        orderA.setDateTime(LocalDateTime.now());

        OrderItem itemB = new OrderItem();
        itemB.setId(2L);
        itemB.setPrice(BigDecimal.valueOf(15));
        itemB.setCount(2);
        Order orderB = new Order();
        orderB.setId(2L);
        orderB.setTotalSum(BigDecimal.valueOf(30));
        orderB.setDateTime(LocalDateTime.now());

        OrderDto dtos = orderMapper.toDto(orderA, List.of(itemA, itemB));

        assertThat(dtos).isNotNull();
        assertThat(dtos.id()).isEqualTo(1L);
        assertThat(dtos.items()).isNotEmpty();
        assertThat(dtos.items().get(0).count()).isEqualTo(1);
        assertThat(dtos.items().get(0).price()).isEqualTo(BigDecimal.TEN);
        assertThat(dtos.items().get(0).total()).isEqualTo(BigDecimal.TEN);
        assertThat(dtos.items().get(1).count()).isEqualTo(2);
        assertThat(dtos.items().get(1).price()).isEqualTo(BigDecimal.valueOf(15));
        assertThat(dtos.items().get(1).total()).isEqualTo(BigDecimal.valueOf(30));
    }

    @Test
    @DisplayName("Маппинг null списка Order")
    void mapNullOrderListTest() {
        OrderDto dtos = orderMapper.toDto(null, null);
        assertThat(dtos).isNull();
    }

    @Test
    @DisplayName("Маппинг пустого списка Order")
    void mapEmptyOrderListTest() {
        OrderDto dtos = orderMapper.toDto(null, List.of());
        assertThat(dtos.id()).isNull();
        assertThat(dtos.totalSum()).isNull();
        assertThat(dtos.dateTime()).isNull();
    }
}
