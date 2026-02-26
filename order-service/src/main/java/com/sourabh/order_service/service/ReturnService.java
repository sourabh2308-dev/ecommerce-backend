package com.sourabh.order_service.service;

import com.sourabh.order_service.common.PageResponse;
import com.sourabh.order_service.dto.request.ReturnRequestDto;
import com.sourabh.order_service.dto.response.ReturnResponse;

public interface ReturnService {

    ReturnResponse requestReturn(String buyerUuid, ReturnRequestDto request);

    ReturnResponse approveReturn(String returnUuid, String adminNotes, Double refundAmount);

    ReturnResponse rejectReturn(String returnUuid, String adminNotes);

    ReturnResponse updateReturnStatus(String returnUuid, String status);

    ReturnResponse getReturn(String returnUuid);

    PageResponse<ReturnResponse> getMyReturns(String buyerUuid, int page, int size);

    PageResponse<ReturnResponse> getAllReturns(String status, int page, int size);
}
