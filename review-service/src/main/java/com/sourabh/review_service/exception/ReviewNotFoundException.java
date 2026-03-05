package com.sourabh.review_service.exception;

/**
 * Thrown when a review lookup by UUID yields no result (or the review has
 * been soft-deleted).
 *
 * <p>Caught by
 * {@link GlobalExceptionHandler#handleNotFound(ReviewNotFoundException)}
 * and mapped to HTTP {@code 404 Not Found}.
 *
 * @see GlobalExceptionHandler
 */
public class ReviewNotFoundException extends RuntimeException {

    /**
     * Constructs a new {@code ReviewNotFoundException} with the given detail message.
     *
     * @param message human-readable description, typically including the unknown UUID
     */
    public ReviewNotFoundException(String message) {
        super(message);
    }
}
