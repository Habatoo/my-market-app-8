package io.github.habatoo.mappers;

import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        uses = {OrderItemMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface OrderMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "totalSum", source = "totalSum")
    @Mapping(target = "dateTime", source = "dateTime")
    OrderDto toDto(Order entity);
}
