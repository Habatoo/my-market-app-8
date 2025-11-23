package io.github.habatoo.mappers;

import io.github.habatoo.dto.response.CartItemDto;
import io.github.habatoo.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        uses = {ItemMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CartItemMapper {

    @Mapping(target = "item", ignore = true)
    CartItemDto toDto(CartItem entity);
}
