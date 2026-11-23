package com.sushilambekar.expensetracker.common;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** A stable JSON shape for one page of results. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.map(mapper).getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
