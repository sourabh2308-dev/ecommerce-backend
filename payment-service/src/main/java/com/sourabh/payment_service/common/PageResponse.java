package com.sourabh.payment_service.common;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Generic wrapper for paginated query results.
 *
 * <p>Every list endpoint in the payment service returns its collection
 * inside a {@code PageResponse} so that clients receive both the current
 * page of data and the metadata required to render pagination controls.
 *
 * <p><b>Example JSON:</b>
 * <pre>{@code
 * {
 *   "content":       [ { ... }, { ... } ],
 *   "page":          0,
 *   "size":          10,
 *   "totalElements": 47,
 *   "totalPages":    5,
 *   "last":          false
 * }
 * }</pre>
 *
 * @param <T> the element type carried in {@link #content}
 */
@Getter
@Builder
public class PageResponse<T> {

    /** Items belonging to the current page. */
    private List<T> content;

    /** Zero-based page index. */
    private int page;

    /** Requested page size (number of items per page). */
    private int size;

    /** Total number of matching items across all pages. */
    private long totalElements;

    /** Total number of pages. */
    private int totalPages;

    /** {@code true} when this is the final page of results. */
    private boolean last;
}
