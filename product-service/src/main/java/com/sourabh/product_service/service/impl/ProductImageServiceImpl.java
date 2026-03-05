package com.sourabh.product_service.service.impl;

import com.sourabh.product_service.dto.request.ImageRequest;
import com.sourabh.product_service.dto.response.ImageResponse;
import com.sourabh.product_service.entity.Product;
import com.sourabh.product_service.entity.ProductImage;
import com.sourabh.product_service.exception.ProductNotFoundException;
import com.sourabh.product_service.repository.ProductImageRepository;
import com.sourabh.product_service.repository.ProductRepository;
import com.sourabh.product_service.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link ProductImageService} for managing product images.
 *
 * <p>Images are stored as URL references with a display order and optional
 * alt text. Only the product's owning seller may add or remove images.
 * When no explicit display order is provided, new images are appended
 * after the last existing image.</p>
 *
 * @see ProductImageService
 * @see ProductImageRepository
 */
@Service
@RequiredArgsConstructor
public class ProductImageServiceImpl implements ProductImageService {

    /** Repository for product entity lookups. */
    private final ProductRepository productRepository;

    /** Repository for product-image persistence. */
    private final ProductImageRepository imageRepository;

    /**
     * {@inheritDoc}
     *
     * <p>If no display order is specified in the request, the image is
     * assigned an order equal to the current image count (i.e. appended
     * at the end).</p>
     */
    @Override
    @Transactional
    public ImageResponse addImage(String productUuid, String sellerUuid, ImageRequest request) {
        Product product = getOwnedProduct(productUuid, sellerUuid);

        int order = (request.getDisplayOrder() != null)
                ? request.getDisplayOrder()
                : imageRepository.countByProductId(product.getId());

        ProductImage image = ProductImage.builder()
                .product(product)
                .imageUrl(request.getImageUrl())
                .displayOrder(order)
                .altText(request.getAltText())
                .build();

        return mapToResponse(imageRepository.save(image));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<ImageResponse> getImages(String productUuid) {
        Product product = productRepository.findByUuidAndIsDeletedFalse(productUuid)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        return imageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId())
                .stream().map(this::mapToResponse).toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public String deleteImage(String productUuid, String sellerUuid, Long imageId) {
        Product product = getOwnedProduct(productUuid, sellerUuid);
        imageRepository.deleteByProductIdAndId(product.getId(), imageId);
        return "Image deleted";
    }

    /**
     * Retrieves a product and verifies seller ownership.
     *
     * @param productUuid the UUID of the product
     * @param sellerUuid  the UUID of the expected owner
     * @return the validated {@link Product}
     * @throws ProductNotFoundException if the product does not exist
     * @throws RuntimeException         if the seller is not the owner
     */
    private Product getOwnedProduct(String productUuid, String sellerUuid) {
        Product product = productRepository.findByUuidAndIsDeletedFalse(productUuid)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        if (!product.getSellerUuid().equals(sellerUuid)) {
            throw new RuntimeException("Not the owner");
        }
        return product;
    }

    /**
     * Maps a {@link ProductImage} entity to an {@link ImageResponse} DTO.
     *
     * @param img the image entity
     * @return the response DTO
     */
    private ImageResponse mapToResponse(ProductImage img) {
        return ImageResponse.builder()
                .id(img.getId())
                .imageUrl(img.getImageUrl())
                .displayOrder(img.getDisplayOrder())
                .altText(img.getAltText())
                .build();
    }
}
