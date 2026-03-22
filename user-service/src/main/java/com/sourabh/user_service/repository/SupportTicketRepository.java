package com.sourabh.user_service.repository;

import com.sourabh.user_service.entity.SupportTicket;
import com.sourabh.user_service.entity.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    Optional<SupportTicket> findByUuid(String uuid);

    Page<SupportTicket> findByUserUuidOrderByCreatedAtDesc(String userUuid, Pageable pageable);

    Page<SupportTicket> findByStatusOrderByCreatedAtAsc(TicketStatus status, Pageable pageable);

    Page<SupportTicket> findByAssignedAdminUuidOrderByCreatedAtAsc(String adminUuid, Pageable pageable);

    Page<SupportTicket> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
