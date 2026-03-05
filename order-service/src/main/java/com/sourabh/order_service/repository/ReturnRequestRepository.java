package com.sourabh.order_service.repository;

import com.sourabh.order_service.entity.ReturnRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ReturnRequest} entities.
 *
 * <p>Supports look-ups by UUID and order UUID, as well as paginated queries
 * filtered by buyer or return status.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see ReturnRequest
 */
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    /**
     * Finds a return request by its public UUID.
     *
     * @param uuid the return request UUID
     * @return an {@link Optional} containing the return request, or empty
     */
    Optional<ReturnRequest> findByUuid(String uuid);

    /**
     * Finds a return request associated with a specific order.
     *
     * @param orderUuid the order UUID
     * @return an {@link Optional} containing the return request, or empty
     */
    Optional<ReturnRequest> findByOrderUuid(String orderUuid);

    /**
     * Returns a page of return requests submitted by the specified buyer.
     *
     * @param buyerUuid the buyer's UUID
     * @param pageable  pagination information
     * @return page of matching {@link ReturnRequest} entities
     */
    Page<ReturnRequest> findByBuyerUuid(String buyerUuid, Pageable pageable);

    /**
     * Returns a page of return requests filtered by their processing status.
     *
     * @param status   the {@link ReturnRequest.ReturnStatus} to filter by
     * @param pageable pagination information
     * @return page of matching {@link ReturnRequest} entities
     */
    Page<ReturnRequest> findByStatus(ReturnRequest.ReturnStatus status, Pageable pageable);
}
