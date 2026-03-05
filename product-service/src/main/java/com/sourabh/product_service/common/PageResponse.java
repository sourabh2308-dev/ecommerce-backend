package com.sourabh.product_service.common;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Generic, offset-based paginated response wrapper used by all product-service
 * list endpoints that support pagination.
 *
 * <p>Wraps the actual data items together with pagination metadata so that
 * clients can render page controls and fetch subsequent pages.
 *
 * <p>Example JSON payload:
 * <pre>{@code
 * {
 *   "content":       [ {…}, {…}, {…} ],
 *   "page":          0,
 *   "size":          10,
 *   "totalElements": 47,
 *   "totalPages":    5,
 *   "last":          false
 * }
 * }</pre>
 *
 * @param <T> the element type contained in {@link #content}
 */
@Getter
@Builder
public class PageResponse<T> {

    /** List of items on the current page. */
    private List<T> content;

    /** Zero-based index of the current page. */
    private int page;

    /** Maximum number of items per page. */
    private int size;

    /** Total number of items across all pages. */
    private long totalElements;

    /** Total number of pages available. */
    private int totalPages;

    /** {@code true} if this is the last page of results. */
    private boolean last;
}
