package io.github.habatoo.mappers;

import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.entity.Item;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ItemMapper {
    List<ItemDto> toDto(List<Item> entities);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "imgPath", source = "imgPath")
    @Mapping(target = "price", source = "price")
    @Mapping(target = "count", source = "count")
    ItemDto toDto(Item entity);
}
