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

/**
 * REST controller for managing product images.
 * <p>
 * Sellers can add and remove images for their own products, while the
 * image listing endpoint is publicly accessible.
 * </p>
 *
 * <p>Base path: {@code /api/product}</p>
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductImageController {

    /** Service encapsulating product-image business logic. */
    private final ProductImageService imageService;

    /**
     * Adds a new image to a product.
     *
     * @param productUuid UUID of the target product
     * @param sellerUuid  UUID of the authenticated seller (from gateway header)
     * @param request     validated payload containing the image URL and metadata
     * @return the newly created image resource
     */
    @PostMapping("/{productUuid}/images")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ImageResponse> addImage(
            @PathVariable String productUuid,
            @RequestHeader("X-User-UUID") String sellerUuid,
            @Valid @RequestBody ImageRequest request) {
        return ResponseEntity.ok(imageService.addImage(productUuid, sellerUuid, request));
    }

    /**
     * Retrieves all images for a product, ordered by display order ascending.
     *
     * @param productUuid UUID of the product
     * @return ordered list of image resources
     */
    @GetMapping("/{productUuid}/images")
    public ResponseEntity<List<ImageResponse>> getImages(@PathVariable String productUuid) {
        return ResponseEntity.ok(imageService.getImages(productUuid));
    }

    /**
     * Deletes a specific image from a product.
     *
     * @param productUuid UUID of the product
     * @param imageId     database ID of the image to remove
     * @param sellerUuid  UUID of the authenticated seller
     * @return confirmation message
     */
    @DeleteMapping("/{productUuid}/images/{imageId}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<String> deleteImage(
            @PathVariable String productUuid,
            @PathVariable Long imageId,
            @RequestHeader("X-User-UUID") String sellerUuid) {
        return ResponseEntity.ok(imageService.deleteImage(productUuid, sellerUuid, imageId));
    }
}
