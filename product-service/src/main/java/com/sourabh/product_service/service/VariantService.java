package com.sourabh.product_service.service;

import com.sourabh.product_service.dto.request.VariantRequest;
import com.sourabh.product_service.dto.response.VariantResponse;

import java.util.List;

public interface VariantService {

    VariantResponse addVariant(String productUuid, String sellerUuid, VariantRequest request);

    List<VariantResponse> getVariants(String productUuid);

    VariantResponse updateVariant(String variantUuid, String sellerUuid, VariantRequest request);

    String deleteVariant(String variantUuid, String sellerUuid);
}
