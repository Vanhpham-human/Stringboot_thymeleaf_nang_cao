package com.example.library.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Bao gói kết quả phân trang cho REST API (ổn định hơn so với serialize trực tiếp PageImpl). */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }
}
