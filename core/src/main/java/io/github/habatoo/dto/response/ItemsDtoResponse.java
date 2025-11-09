package io.github.habatoo.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record ItemsDtoResponse(
        List<List<ItemDto>> itemsRows,
        CartDto cart,
        Paging paging
) {
}
