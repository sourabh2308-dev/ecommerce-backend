package com.sourabh.product_service.service;

import com.sourabh.product_service.dto.request.ImageRequest;
import com.sourabh.product_service.dto.response.ImageResponse;

import java.util.List;

/**
 * Service interface for managing product images.
 *
 * <p>Each product can have multiple images, each with a display order
 * and optional alt text. Only the product's owning seller may add or
 * remove images.</p>
 *
 * @see com.sourabh.product_service.service.impl.ProductImageServiceImpl
 * @see com.sourabh.product_service.entity.ProductImage
 */
public interface ProductImageService {

    /**
     * Adds an image to a product.
     *
     * <p>If no display order is specified in the request, the image is
     * appended at the end of the existing image list.</p>
     *
     * @param productUuid the UUID of the product
     * @param sellerUuid  the UUID of the seller (must own the product)
     * @param request     image URL, optional display order, and alt text
     * @return the newly created {@link ImageResponse}
     */
    ImageResponse addImage(String productUuid, String sellerUuid, ImageRequest request);

    /**
     * Retrieves all images for a product, ordered by display order ascending.
     *
     * @param productUuid the UUID of the product
     * @return list of {@link ImageResponse} objects
     */
    List<ImageResponse> getImages(String productUuid);

    /**
     * Deletes a specific image from a product.
     *
     * @param productUuid the UUID of the product
     * @param sellerUuid  the UUID of the seller (must own the product)
     * @param imageId     the database ID of the image to remove
     * @return confirmation message
     */
    String deleteImage(String productUuid, String sellerUuid, Long imageId);
}
