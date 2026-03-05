package com.sourabh.review_service.exception;

/**
 * Thrown when a buyer attempts to create a duplicate review for a product
 * they have already reviewed.
 *
 * <p>Business rule: each buyer may submit at most one review per product.
 * The check is performed in
 * {@link com.sourabh.review_service.service.impl.ReviewServiceImpl#createReview}.
 *
 * <p>Caught by
 * {@link GlobalExceptionHandler#handleDuplicate(ReviewAlreadyExistsException)}
 * and mapped to HTTP {@code 409 Conflict}.
 *
 * @see GlobalExceptionHandler
 */
public class ReviewAlreadyExistsException extends RuntimeException {

    /**
     * Constructs a new {@code ReviewAlreadyExistsException} with the given detail message.
     *
     * @param message human-readable description of the duplication conflict
     */
    public ReviewAlreadyExistsException(String message) { super(message); }
}
