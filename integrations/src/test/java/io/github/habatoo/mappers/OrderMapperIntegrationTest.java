package io.github.habatoo.mappers;

import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.dto.response.OrderItemDto;
import io.github.habatoo.entity.Order;
import io.github.habatoo.entity.OrderItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderMapperIntegrationTest {
    @Autowired
    private OrderMapper orderMapper;

    @Test
    @DisplayName("Маппинг Order в OrderDto с позициями")
    void mapOrderToDtoTest() {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(21L);
        orderItem.setPrice(BigDecimal.valueOf(14));
        orderItem.setCount(2);

        Order order = new Order();
        order.setId(100L);
        order.setItems(List.of(orderItem));
        order.setTotalSum(BigDecimal.valueOf(28));
        order.setDateTime(LocalDateTime.of(2025, 11, 12, 15, 18));

        OrderDto dto = orderMapper.toDto(order);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(100L);
        assertThat(dto.totalSum()).isEqualTo(BigDecimal.valueOf(28));
        assertThat(dto.dateTime()).isEqualTo(LocalDateTime.of(2025, 11, 12, 15, 18));
        assertThat(dto.items()).hasSize(1);

        OrderItemDto itemDto = dto.items().get(0);
        assertThat(itemDto.price()).isEqualTo(BigDecimal.valueOf(14));
        assertThat(itemDto.count()).isEqualTo(2);
        assertThat(itemDto.total()).isEqualTo(BigDecimal.valueOf(28));
    }

    @Test
    @DisplayName("Маппинг списка Order в список OrderDto")
    void mapOrderListTest() {
        OrderItem itemA = new OrderItem();
        itemA.setId(1L);
        itemA.setPrice(BigDecimal.valueOf(10));
        itemA.setCount(1);
        Order orderA = new Order();
        orderA.setId(1L);
        orderA.setItems(List.of(itemA));
        orderA.setTotalSum(BigDecimal.valueOf(10));
        orderA.setDateTime(LocalDateTime.now());

        OrderItem itemB = new OrderItem();
        itemB.setId(2L);
        itemB.setPrice(BigDecimal.valueOf(15));
        itemB.setCount(2);
        Order orderB = new Order();
        orderB.setId(2L);
        orderB.setItems(List.of(itemB));
        orderB.setTotalSum(BigDecimal.valueOf(30));
        orderB.setDateTime(LocalDateTime.now());

        List<OrderDto> dtos = orderMapper.toDto(List.of(orderA, orderB));

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).id()).isEqualTo(1L);
        assertThat(dtos.get(1).id()).isEqualTo(2L);
        assertThat(dtos.get(0).totalSum()).isEqualTo(BigDecimal.valueOf(10));
        assertThat(dtos.get(1).totalSum()).isEqualTo(BigDecimal.valueOf(30));
    }

    @Test
    @DisplayName("Маппинг пустого списка Order")
    void mapEmptyOrderListTest() {
        assertThat(orderMapper.toDto(List.of())).isEmpty();
    }
}
