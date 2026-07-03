package com.comandago.api.shared.util;

import com.comandago.api.shared.response.PageResponse;
import org.springframework.data.domain.Page;

public final class PaginationUtils {

    private PaginationUtils() {
    }

    public static <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
