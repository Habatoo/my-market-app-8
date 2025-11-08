package io.github.habatoo.mappers;

import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.entity.Cart;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {CartItemMapper.class}
)
public interface CartMapper {
    List<CartDto> toDto(List<Cart> entities);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "items", source = "items")
    @Mapping(target = "total", source = "total")
    CartDto toDto(Cart entity);
}
