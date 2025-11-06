package io.github.habatoo.mappers;

import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.entity.Item;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ItemMapper extends BaseMapper<Item, ItemDto> {
}
