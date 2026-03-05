package com.sourabh.user_service.repository;

import com.sourabh.user_service.entity.SupportTicket;
import com.sourabh.user_service.entity.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link SupportTicket} entities.
 * <p>
 * Supports look-ups by UUID, user UUID, ticket status, and assigned admin,
 * with pagination and chronological ordering.
 * </p>
 *
 * @see SupportTicket
 */
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    /**
     * Finds a support ticket by its public UUID.
     *
     * @param uuid the unique public identifier
     * @return the matching ticket, if present
     */
    Optional<SupportTicket> findByUuid(String uuid);

    /**
     * Returns a paginated list of tickets belonging to a specific user,
     * sorted newest-first.
     *
     * @param userUuid the user's UUID
     * @param pageable pagination parameters
     * @return a page of tickets owned by that user
     */
    Page<SupportTicket> findByUserUuidOrderByCreatedAtDesc(String userUuid, Pageable pageable);

    /**
     * Returns a paginated list of tickets filtered by status,
     * sorted oldest-first (FIFO for support agents).
     *
     * @param status   the {@link TicketStatus} to filter by
     * @param pageable pagination parameters
     * @return a page of tickets matching the status
     */
    Page<SupportTicket> findByStatusOrderByCreatedAtAsc(TicketStatus status, Pageable pageable);

    /**
     * Returns a paginated list of tickets assigned to a specific admin,
     * sorted oldest-first.
     *
     * @param adminUuid the admin's UUID
     * @param pageable  pagination parameters
     * @return a page of tickets assigned to the admin
     */
    Page<SupportTicket> findByAssignedAdminUuidOrderByCreatedAtAsc(String adminUuid, Pageable pageable);

    /**
     * Returns a paginated list of all support tickets,
     * sorted newest-first.
     *
     * @param pageable pagination parameters
     * @return a page of all tickets
     */
    Page<SupportTicket> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
