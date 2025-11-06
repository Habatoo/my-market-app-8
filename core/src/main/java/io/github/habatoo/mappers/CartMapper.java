package io.github.habatoo.mappers;

import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.entity.Cart;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CartMapper extends BaseMapper<Cart, CartDto> {
}
