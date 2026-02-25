package com.sourabh.user_service.common;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
/**
 * DATA TRANSFER OBJECT (DTO) - Server Response Format
 * 
 * Defines the JSON structure returned to HTTP clients.
 * Built from Entity objects via mapper methods.
 * May include computed fields not in database.
 */
/**
 * PAGE RESPONSE DTO - Paginated Results Container
 * 
 * PURPOSE:
 * Wraps paginated list results with metadata about pagination.
 * Used by all list endpoints that support pagination.
 * 
 * FIELDS:
 * - content: List of items (orders, products, users, etc.)
 * - page: Current page number (0-indexed)
 * - size: Items per page
 * - totalElements: Total count across all pages
 * - totalPages: Total number of pages
 * - last: Boolean indicating if this is the last page
 * 
 * EXAMPLE JSON:
 * {
 *   "content": [{...}, {...}, {...}],
 *   "page": 0,
 *   "size": 10,
 *   "totalElements": 47,
 *   "totalPages": 5,
 *   "last": false
 * }
 * 
 * CLIENT USAGE:
 * - Display items from "content" array
 * - Show page indicator: "Page 1 of 5"
 * - Enable/disable next/prev buttons based on "last" and "page"
 */
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
