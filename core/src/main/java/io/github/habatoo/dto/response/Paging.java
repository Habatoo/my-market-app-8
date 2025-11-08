package io.github.habatoo.dto.response;

public record Paging(int total, int pageSize, int pageNumber, boolean hasPrevious, boolean hasNext) {
}
