package com.sourabh.user_service.common;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Generic paginated response wrapper used for any list endpoint that supports
 * paging (e.g. user listings, notifications, loyalty transactions).
 *
 * <p>Maps directly from a Spring Data {@code Page<T>} object so that
 * clients receive consistent pagination metadata.</p>
 *
 * <p><b>Example JSON:</b></p>
 * <pre>
 * {
 *   "content": [ ... ],
 *   "page": 0,
 *   "size": 10,
 *   "totalElements": 47,
 *   "totalPages": 5,
 *   "last": false
 * }
 * </pre>
 *
 * @param <T> the type of the elements contained in {@link #content}
 */
@Getter
@Builder
public class PageResponse<T> {

    /** Items on the current page. */
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
