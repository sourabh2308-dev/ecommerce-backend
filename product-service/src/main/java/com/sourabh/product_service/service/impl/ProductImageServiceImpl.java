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

@Service
@RequiredArgsConstructor
public class ProductImageServiceImpl implements ProductImageService {

    private final ProductRepository productRepository;
    private final ProductImageRepository imageRepository;

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

    @Override
    @Transactional(readOnly = true)
    public List<ImageResponse> getImages(String productUuid) {
        Product product = productRepository.findByUuidAndIsDeletedFalse(productUuid)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        return imageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId())
                .stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional
    public String deleteImage(String productUuid, String sellerUuid, Long imageId) {
        Product product = getOwnedProduct(productUuid, sellerUuid);
        imageRepository.deleteByProductIdAndId(product.getId(), imageId);
        return "Image deleted";
    }

    private Product getOwnedProduct(String productUuid, String sellerUuid) {
        Product product = productRepository.findByUuidAndIsDeletedFalse(productUuid)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        if (!product.getSellerUuid().equals(sellerUuid)) {
            throw new RuntimeException("Not the owner");
        }
        return product;
    }

    private ImageResponse mapToResponse(ProductImage img) {
        return ImageResponse.builder()
                .id(img.getId())
                .imageUrl(img.getImageUrl())
                .displayOrder(img.getDisplayOrder())
                .altText(img.getAltText())
                .build();
    }
}
