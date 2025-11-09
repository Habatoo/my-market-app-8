package io.github.habatoo.dto.response;

import lombok.Builder;

@Builder
public record ItemDtoResponse(
        ItemDto item,
        Integer cartCount
) {
}
