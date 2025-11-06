package io.github.habatoo.mappers;

import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper extends BaseMapper<Order, OrderDto> {
}
