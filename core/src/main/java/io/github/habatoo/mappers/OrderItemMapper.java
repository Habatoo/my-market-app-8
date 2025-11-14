package io.github.habatoo.mappers;

import io.github.habatoo.dto.response.OrderItemDto;
import io.github.habatoo.entity.OrderItem;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;

import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {ItemMapper.class})
public interface OrderItemMapper {

    @Mapping(target = "order", ignore = true)
    @Mapping(target = "total", ignore = true)
    OrderItemDto toDto(OrderItem entity);

    List<OrderItemDto> toDto(List<OrderItem> entities);

    @AfterMapping
    default void fillTotal(OrderItem entity, @MappingTarget OrderItemDto.OrderItemDtoBuilder dto) {
        BigDecimal price = entity.getPrice() != null ? entity.getPrice() : BigDecimal.ZERO;
        int count = entity.getCount() != null ? entity.getCount() : 0;
        dto.total(price.multiply(BigDecimal.valueOf(count)));
    }
}
