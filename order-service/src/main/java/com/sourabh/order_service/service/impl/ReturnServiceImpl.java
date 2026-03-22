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

@Service
@RequiredArgsConstructor
public class ReturnServiceImpl implements ReturnService {

    private final ReturnRequestRepository returnRequestRepository;
    
    private final OrderRepository orderRepository;

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

    @Override
    @Transactional
    public ReturnResponse approveReturn(String returnUuid, String adminNotes, Double refundAmount) {
        ReturnRequest rr = findReturn(returnUuid);
        rr.setStatus(ReturnRequest.ReturnStatus.APPROVED);
        rr.setAdminNotes(adminNotes);
        if (refundAmount != null) rr.setRefundAmount(refundAmount);
        return mapToResponse(returnRequestRepository.save(rr));
    }

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

    @Override
    @Transactional(readOnly = true)
    public ReturnResponse getReturn(String returnUuid) {
        return mapToResponse(findReturn(returnUuid));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReturnResponse> getMyReturns(String buyerUuid, int page, int size) {
        Page<ReturnRequest> pg = returnRequestRepository.findByBuyerUuid(buyerUuid,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return toPageResponse(pg);
    }

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

    private ReturnRequest findReturn(String uuid) {
        return returnRequestRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Return request not found"));
    }

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
