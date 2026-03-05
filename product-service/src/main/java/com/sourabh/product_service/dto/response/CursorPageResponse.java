package com.sourabh.product_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Cursor-based pagination response.
 *
 * <p>Unlike offset-based pagination ({@link com.sourabh.product_service.common.PageResponse}),
 * cursor pagination uses the last returned item's identifier as a bookmark for
 * the next page.  This avoids duplicated or skipped rows when the underlying
 * data changes between page requests.
 *
 * @param <T> the element type contained in {@link #content}
 */
@Getter
@Builder
public class CursorPageResponse<T> {

    /** Items on the current page. */
    private List<T> content;

    /** Maximum number of items per page. */
    private int size;

    /** Total number of matching items across all pages. */
    private long totalElements;

    /** {@code true} if more pages are available after this one. */
    private boolean hasNext;

    /** Opaque cursor to pass when requesting the next page; {@code null} when there are no more pages. */
    private String nextCursor;
}
