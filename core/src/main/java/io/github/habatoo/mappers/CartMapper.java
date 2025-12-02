package io.github.habatoo.mappers;

import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.entity.Cart;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CartItemMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CartMapper {

    @Mapping(target = "items", ignore = true)
    CartDto toDto(Cart entity);

    List<CartDto> toDto(List<Cart> entities);
}
