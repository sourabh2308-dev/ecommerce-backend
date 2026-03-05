package com.sourabh.payment_service.repository;

import com.sourabh.payment_service.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Payment} entities.
 *
 * <p>Provides standard CRUD operations plus custom finders for UUID-based
 * lookups, buyer-scoped pagination, and a seller-scoped query that
 * identifies payments containing at least one split for a given seller.
 */
public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    /**
     * Finds a payment by its public UUID.
     *
     * @param uuid the payment UUID
     * @return the payment, or empty if not found
     */
    Optional<Payment> findByUuid(String uuid);

    /**
     * Checks whether a payment already exists for the given order UUID.
     * Used as an idempotency guard in the Kafka consumer.
     *
     * @param orderUuid the order UUID to check
     * @return {@code true} if a payment exists
     */
    boolean existsByOrderUuid(String orderUuid);

    /**
     * Finds the payment associated with a specific order.
     *
     * @param orderUuid the order UUID
     * @return the payment, or empty if not found
     */
    Optional<Payment> findByOrderUuid(String orderUuid);

    /**
     * Finds a payment by the external gateway order ID (e.g. Razorpay
     * {@code order_xxx}).  Used to correlate webhook callbacks.
     *
     * @param gatewayOrderId the gateway-supplied order identifier
     * @return the payment, or empty if not found
     */
    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);

    /**
     * Returns a paginated list of payments belonging to a specific buyer.
     *
     * @param buyerUuid the buyer UUID
     * @param pageable  pagination and sort parameters
     * @return a page of payments
     */
    Page<Payment> findByBuyerUuid(String buyerUuid, Pageable pageable);

    /**
     * Returns a paginated list of payments that contain at least one
     * {@code PaymentSplit} for the specified seller.
     *
     * <p>Uses a JPQL sub-select to avoid a full join.
     *
     * @param sellerUuid the seller UUID
     * @param pageable   pagination and sort parameters
     * @return a page of payments
     */
    @Query("SELECT DISTINCT p FROM Payment p " +
           "WHERE EXISTS (SELECT 1 FROM PaymentSplit ps WHERE ps.paymentUuid = p.uuid AND ps.sellerUuid = :sellerUuid)")
    Page<Payment> findBySellerUuid(@Param("sellerUuid") String sellerUuid, Pageable pageable);
}
