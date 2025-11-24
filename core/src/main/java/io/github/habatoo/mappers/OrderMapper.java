package io.github.habatoo.mappers;

import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.entity.Order;
import io.github.habatoo.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {OrderItemMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface OrderMapper {

    @Mapping(target = "id", source = "order.id")
    @Mapping(target = "items", source = "items")
    @Mapping(target = "totalSum", source = "order.totalSum")
    @Mapping(target = "dateTime", source = "order.dateTime")
    OrderDto toDto(Order order, List<OrderItem> items);
}
