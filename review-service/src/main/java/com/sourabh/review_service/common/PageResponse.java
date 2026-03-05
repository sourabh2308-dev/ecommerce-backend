package com.sourabh.review_service.common;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Generic wrapper for paginated query results returned by review list endpoints.
 *
 * <p>Encapsulates the current page of items together with pagination metadata
 * (page number, page size, total elements, total pages, last-page flag) so
 * that API consumers can build page navigation without additional calls.
 *
 * <p>Used by {@link com.sourabh.review_service.controller.ReviewController}
 * for the {@code GET /api/review/product/{productUuid}} and
 * {@code GET /api/review/me} endpoints.
 *
 * @param <T> the element type contained in each page (e.g.&nbsp;{@code ReviewResponse})
 */
@Getter
@Builder
public class PageResponse<T> {

    /** The items belonging to the current page of results. */
    private List<T> content;

    /** Zero-based index of the current page. */
    private int page;

    /** Maximum number of items requested per page. */
    private int size;

    /** Total number of matching items across all pages. */
    private long totalElements;

    /** Total number of available pages. */
    private int totalPages;

    /** {@code true} when the current page is the final page of results. */
    private boolean last;
}
