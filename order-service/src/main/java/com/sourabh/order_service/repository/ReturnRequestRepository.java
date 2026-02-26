package com.sourabh.order_service.repository;

import com.sourabh.order_service.entity.ReturnRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    Optional<ReturnRequest> findByUuid(String uuid);

    Optional<ReturnRequest> findByOrderUuid(String orderUuid);

    Page<ReturnRequest> findByBuyerUuid(String buyerUuid, Pageable pageable);

    Page<ReturnRequest> findByStatus(ReturnRequest.ReturnStatus status, Pageable pageable);
}
