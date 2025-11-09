package io.github.habatoo.dto.response;

import lombok.Builder;

@Builder
public record Paging(
        int total,
        int pageSize,
        int pageNumber,
        boolean hasPrevious,
        boolean hasNext) {
}
