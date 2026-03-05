package com.sourabh.review_service.exception;

/**
 * Thrown when a user attempts an operation on a review they are not
 * authorised to perform.
 *
 * <p>Typical scenarios:
 * <ul>
 *   <li>A buyer tries to update or delete another buyer's review.</li>
 *   <li>A buyer attempts to review an order that belongs to someone else.</li>
 *   <li>A buyer tries to add an image to another buyer's review.</li>
 * </ul>
 *
 * <p>Caught by
 * {@link GlobalExceptionHandler#handleAccess(ReviewAccessException)} and
 * mapped to HTTP {@code 403 Forbidden}.
 *
 * @see GlobalExceptionHandler
 */
public class ReviewAccessException extends RuntimeException {

    /**
     * Constructs a new {@code ReviewAccessException} with the given detail message.
     *
     * @param message human-readable description of the access violation
     */
    public ReviewAccessException(String message) { super(message); }
}
