package io.github.habatoo.mappers;

import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {OrderItemMapper.class}
)
public interface OrderMapper {
    List<OrderDto> toDto(List<Order> entities);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "items", source = "items")
    @Mapping(target = "totalSum", source = "totalSum")
    @Mapping(target = "dateTime", source = "dateTime")
    OrderDto toDto(Order entity);
}
