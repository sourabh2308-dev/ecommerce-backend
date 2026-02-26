package com.sourabh.product_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Cursor-based pagination response.
 * Uses the last item's ID as cursor for the next page, avoiding offset-based pagination issues.
 */
@Getter
@Builder
public class CursorPageResponse<T> {
    private List<T> content;
    private int size;
    private long totalElements;
    private boolean hasNext;
    /** Cursor to pass for the next page (last item's ID). Null if no more pages. */
    private String nextCursor;
}
