package com.sourabh.product_service.service;

import com.sourabh.product_service.dto.request.ImageRequest;
import com.sourabh.product_service.dto.response.ImageResponse;

import java.util.List;

public interface ProductImageService {

    ImageResponse addImage(String productUuid, String sellerUuid, ImageRequest request);

    List<ImageResponse> getImages(String productUuid);

    String deleteImage(String productUuid, String sellerUuid, Long imageId);
}
