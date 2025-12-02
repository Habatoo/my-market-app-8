package io.github.habatoo.mappers;

import io.github.habatoo.dto.response.OrderItemDto;
import io.github.habatoo.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;

@Mapper(componentModel = "spring",
        uses = {ItemMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = {BigDecimal.class})
public interface OrderItemMapper {

    @Mapping(target = "item", ignore = true)
    @Mapping(target = "total", expression = "java(entity.getPrice().multiply(BigDecimal.valueOf(entity.getCount())))")
    OrderItemDto toDto(OrderItem entity);
}
