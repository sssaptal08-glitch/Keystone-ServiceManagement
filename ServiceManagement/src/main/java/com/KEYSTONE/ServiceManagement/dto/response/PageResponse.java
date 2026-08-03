package com.KEYSTONE.ServiceManagement.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic pagination envelope so list endpoints never return unbounded result sets and the
 * frontend always knows how many pages exist, regardless of which resource it's paging through.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
