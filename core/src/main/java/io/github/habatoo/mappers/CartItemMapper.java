package io.github.habatoo.mappers;

import io.github.habatoo.dto.response.CartItemDto;
import io.github.habatoo.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;


@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {ItemMapper.class})
public interface CartItemMapper {
    List<CartItemDto> toDto(List<CartItem> entities);

    @Mapping(target = "item", source = "item")
    @Mapping(target = "count", source = "count")
    @Mapping(target = "price", source = "price")
    CartItemDto toDto(CartItem entity);
}
