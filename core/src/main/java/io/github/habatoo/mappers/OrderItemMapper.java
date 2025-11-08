package io.github.habatoo.mappers;

import io.github.habatoo.dto.response.OrderItemDto;
import io.github.habatoo.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {ItemMapper.class})
public interface OrderItemMapper {
    List<OrderItemDto> toDto(List<OrderItem> entities);

    @Mapping(target = "item", source = "item")
    @Mapping(target = "count", source = "count")
    @Mapping(target = "price", source = "price")
    @Mapping(target = "order", ignore = true)
    OrderItemDto toDto(OrderItem entity);
}
