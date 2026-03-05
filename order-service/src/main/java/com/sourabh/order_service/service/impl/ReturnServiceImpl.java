package com.sourabh.order_service.service.impl;

import com.sourabh.order_service.common.PageResponse;
import com.sourabh.order_service.dto.request.ReturnRequestDto;
import com.sourabh.order_service.dto.response.ReturnResponse;
import com.sourabh.order_service.entity.Order;
import com.sourabh.order_service.entity.OrderStatus;
import com.sourabh.order_service.entity.ReturnRequest;
import com.sourabh.order_service.entity.ReturnType;
import com.sourabh.order_service.repository.OrderRepository;
import com.sourabh.order_service.repository.ReturnRequestRepository;
import com.sourabh.order_service.service.ReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Manages the lifecycle of order return and exchange requests.
 *
 * <p>Supports two return types: {@code REFUND} (money back) and
 * {@code EXCHANGE} (replacement product). Requests follow the status
 * progression {@code REQUESTED → APPROVED/REJECTED → REFUNDED/EXCHANGED},
 * with each transition synchronising the parent order's status.</p>
 *
 * <p>Only {@code DELIVERED} orders are eligible for return, and duplicate
 * return requests for the same order are rejected.</p>
 *
 * @see ReturnService
 * @see ReturnRequest
 */
@Service
@RequiredArgsConstructor
public class ReturnServiceImpl implements ReturnService {

    /** JPA repository for {@link ReturnRequest} persistence. */
    private final ReturnRequestRepository returnRequestRepository;
    /** JPA repository for querying and updating parent orders. */
    private final OrderRepository orderRepository;

    /**
     * {@inheritDoc}
     *
     * <p>Validates order ownership, delivery status, and duplicate-return checks
     * before creating the {@link ReturnRequest}. The parent order status is set
     * to {@code RETURN_REQUESTED} and the refund amount defaults to the full
     * order total (overridable later by an admin).</p>
     *
     * @throws RuntimeException if the order is not found, not owned by the buyer,
     *                          not delivered, or already has a return request
     */
    @Override
    @Transactional
    public ReturnResponse requestReturn(String buyerUuid, ReturnRequestDto request) {
        Order order = orderRepository.findByUuidAndIsDeletedFalse(request.getOrderUuid())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!order.getBuyerUuid().equals(buyerUuid)) {
            throw new RuntimeException("Not your order");
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new RuntimeException("Can only return delivered orders");
        }
        if (returnRequestRepository.findByOrderUuid(request.getOrderUuid()).isPresent()) {
            throw new RuntimeException("Return already requested for this order");
        }

        order.setStatus(OrderStatus.RETURN_REQUESTED);
        order.setReturnType(ReturnType.valueOf(request.getReturnType()));
        order.setReturnReason(request.getReason());
        orderRepository.save(order);

        ReturnRequest rr = ReturnRequest.builder()
                .orderUuid(request.getOrderUuid())
                .buyerUuid(buyerUuid)
                .returnType(ReturnType.valueOf(request.getReturnType()))
                .reason(request.getReason())
                .refundAmount(order.getTotalAmount())
                .build();
        return mapToResponse(returnRequestRepository.save(rr));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sets status to {@code APPROVED}. If {@code refundAmount} is non-null
     * it overrides the original amount, enabling partial refunds.</p>
     */
    @Override
    @Transactional
    public ReturnResponse approveReturn(String returnUuid, String adminNotes, Double refundAmount) {
        ReturnRequest rr = findReturn(returnUuid);
        rr.setStatus(ReturnRequest.ReturnStatus.APPROVED);
        rr.setAdminNotes(adminNotes);
        if (refundAmount != null) rr.setRefundAmount(refundAmount);
        return mapToResponse(returnRequestRepository.save(rr));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sets status to {@code REJECTED}, records a resolution timestamp,
     * and moves the parent order to {@code RETURN_REJECTED}.</p>
     */
    @Override
    @Transactional
    public ReturnResponse rejectReturn(String returnUuid, String adminNotes) {
        ReturnRequest rr = findReturn(returnUuid);
        rr.setStatus(ReturnRequest.ReturnStatus.REJECTED);
        rr.setAdminNotes(adminNotes);
        rr.setResolvedAt(LocalDateTime.now());

        orderRepository.findByUuidAndIsDeletedFalse(rr.getOrderUuid())
                .ifPresent(order -> {
                    order.setStatus(OrderStatus.RETURN_REJECTED);
                    orderRepository.save(order);
                });

        return mapToResponse(returnRequestRepository.save(rr));
    }

    /**
     * {@inheritDoc}
     *
     * <p>When the new status is {@code REFUNDED} or {@code EXCHANGED}, a
     * resolution timestamp is recorded and the parent order status is
     * updated to {@code REFUND_ISSUED} or {@code EXCHANGE_ISSUED}
     * respectively.</p>
     */
    @Override
    @Transactional
    public ReturnResponse updateReturnStatus(String returnUuid, String status) {
        ReturnRequest rr = findReturn(returnUuid);
        rr.setStatus(ReturnRequest.ReturnStatus.valueOf(status));
        if (status.equals("REFUNDED") || status.equals("EXCHANGED")) {
            rr.setResolvedAt(LocalDateTime.now());
            orderRepository.findByUuidAndIsDeletedFalse(rr.getOrderUuid())
                    .ifPresent(order -> {
                        order.setStatus(status.equals("REFUNDED") ? OrderStatus.REFUND_ISSUED : OrderStatus.EXCHANGE_ISSUED);
                        orderRepository.save(order);
                    });
        }
        return mapToResponse(returnRequestRepository.save(rr));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public ReturnResponse getReturn(String returnUuid) {
        return mapToResponse(findReturn(returnUuid));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReturnResponse> getMyReturns(String buyerUuid, int page, int size) {
        Page<ReturnRequest> pg = returnRequestRepository.findByBuyerUuid(buyerUuid,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return toPageResponse(pg);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReturnResponse> getAllReturns(String status, int page, int size) {
        Page<ReturnRequest> pg;
        if (status != null && !status.isBlank()) {
            pg = returnRequestRepository.findByStatus(ReturnRequest.ReturnStatus.valueOf(status),
                    PageRequest.of(page, size, Sort.by("createdAt").descending()));
        } else {
            pg = returnRequestRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()));
        }
        return toPageResponse(pg);
    }

    /**
     * Looks up a {@link ReturnRequest} by UUID.
     *
     * @param uuid return-request UUID
     * @return the entity
     * @throws RuntimeException if not found
     */
    private ReturnRequest findReturn(String uuid) {
        return returnRequestRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Return request not found"));
    }

    /**
     * Converts a Spring Data {@link Page} of return requests into a
     * {@link PageResponse} DTO.
     */
    private PageResponse<ReturnResponse> toPageResponse(Page<ReturnRequest> pg) {
        List<ReturnResponse> content = pg.getContent().stream().map(this::mapToResponse).toList();
        return PageResponse.<ReturnResponse>builder()
                .content(content)
                .page(pg.getNumber())
                .size(pg.getSize())
                .totalElements(pg.getTotalElements())
                .totalPages(pg.getTotalPages())
                .last(pg.isLast())
                .build();
    }

    /** Maps a {@link ReturnRequest} entity to its API response DTO. */
    private ReturnResponse mapToResponse(ReturnRequest rr) {
        return ReturnResponse.builder()
                .uuid(rr.getUuid())
                .orderUuid(rr.getOrderUuid())
                .buyerUuid(rr.getBuyerUuid())
                .returnType(rr.getReturnType().name())
                .reason(rr.getReason())
                .status(rr.getStatus().name())
                .adminNotes(rr.getAdminNotes())
                .refundAmount(rr.getRefundAmount())
                .resolvedAt(rr.getResolvedAt())
                .createdAt(rr.getCreatedAt())
                .build();
    }
}
