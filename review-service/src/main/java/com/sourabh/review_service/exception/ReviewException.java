package com.sourabh.review_service.exception;

/**
 * General-purpose exception for review business-rule violations that do
 * not fit into more specific categories.
 *
 * <p>Common scenarios:
 * <ul>
 *   <li>The referenced order has not been delivered yet.</li>
 *   <li>The product UUID is not part of the referenced order.</li>
 *   <li>The order-service is unavailable (circuit-breaker fallback).</li>
 *   <li>Maximum image limit per review exceeded.</li>
 * </ul>
 *
 * <p>Caught by
 * {@link GlobalExceptionHandler#handleReviewException(ReviewException)}
 * and mapped to HTTP {@code 400 Bad Request}.
 *
 * @see GlobalExceptionHandler
 */
public class ReviewException extends RuntimeException {

    /**
     * Constructs a new {@code ReviewException} with the given detail message.
     *
     * @param message human-readable description of the business-rule violation
     */
    public ReviewException(String message) { super(message); }
}
