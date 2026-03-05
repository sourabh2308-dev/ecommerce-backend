package com.sourabh.order_service.common;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Generic paginated response wrapper used by list endpoints that support
 * pagination within the order-service.
 *
 * <p>Encapsulates a page of results together with metadata about the
 * current page position, total element count, and whether additional pages
 * exist. Clients use this information to render pagination controls.</p>
 *
 * <p><b>Example JSON:</b></p>
 * <pre>
 * {
 *   "content": [{...}, {...}],
 *   "page": 0,
 *   "size": 10,
 *   "totalElements": 47,
 *   "totalPages": 5,
 *   "last": false
 * }
 * </pre>
 *
 * @param <T> the type of elements contained in this page
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

    /** Total number of elements across all pages. */
    private long totalElements;

    /** Total number of available pages. */
    private int totalPages;

    /** {@code true} if this is the last page of results. */
    private boolean last;
}
