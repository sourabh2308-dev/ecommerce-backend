package com.sourabh.product_service.controller;

import com.sourabh.product_service.dto.request.ImageRequest;
import com.sourabh.product_service.dto.response.ImageResponse;
import com.sourabh.product_service.service.ProductImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductImageController {

    private final ProductImageService imageService;

    @PostMapping("/{productUuid}/images")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ImageResponse> addImage(
            @PathVariable String productUuid,
            @RequestHeader("X-User-UUID") String sellerUuid,
            @Valid @RequestBody ImageRequest request) {
        return ResponseEntity.ok(imageService.addImage(productUuid, sellerUuid, request));
    }

    @GetMapping("/{productUuid}/images")
    public ResponseEntity<List<ImageResponse>> getImages(@PathVariable String productUuid) {
        return ResponseEntity.ok(imageService.getImages(productUuid));
    }

    @DeleteMapping("/{productUuid}/images/{imageId}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<String> deleteImage(
            @PathVariable String productUuid,
            @PathVariable Long imageId,
            @RequestHeader("X-User-UUID") String sellerUuid) {
        return ResponseEntity.ok(imageService.deleteImage(productUuid, sellerUuid, imageId));
    }
}
