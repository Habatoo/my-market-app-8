package io.github.habatoo.mappers;

import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.entity.Item;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ItemMapper {
    List<ItemDto> toDto(List<Item> entities);

    @Mapping(target = "count", constant = "0")
    ItemDto toDto(Item entity);
}
