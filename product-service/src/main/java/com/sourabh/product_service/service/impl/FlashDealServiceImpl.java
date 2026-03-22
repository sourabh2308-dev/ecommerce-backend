package com.sourabh.product_service.service.impl;

import com.sourabh.product_service.dto.request.FlashDealRequest;
import com.sourabh.product_service.dto.response.FlashDealResponse;
import com.sourabh.product_service.entity.FlashDeal;
import com.sourabh.product_service.entity.Product;
import com.sourabh.product_service.exception.ProductNotFoundException;
import com.sourabh.product_service.repository.FlashDealRepository;
import com.sourabh.product_service.repository.ProductRepository;
import com.sourabh.product_service.service.FlashDealService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlashDealServiceImpl implements FlashDealService {

    private final FlashDealRepository dealRepository;

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public FlashDealResponse createDeal(String sellerUuid, FlashDealRequest request) {
        Product product = productRepository.findByUuidAndIsDeletedFalse(request.getProductUuid())
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        if (!product.getSellerUuid().equals(sellerUuid)) {
            throw new RuntimeException("Not the owner of this product");
        }
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        FlashDeal deal = FlashDeal.builder()
                .product(product)
                .discountPercent(request.getDiscountPercent())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .sellerUuid(sellerUuid)
                .build();

        return mapToResponse(dealRepository.save(deal), product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlashDealResponse> getActiveDeals() {
        return dealRepository.findAllActive(LocalDateTime.now()).stream()
                .map(d -> mapToResponse(d, d.getProduct()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlashDealResponse> getMyDeals(String sellerUuid) {
        return dealRepository.findBySellerUuidOrderByCreatedAtDesc(sellerUuid).stream()
                .map(d -> mapToResponse(d, d.getProduct()))
                .toList();
    }

    @Override
    @Transactional
    public String cancelDeal(String dealUuid, String sellerUuid) {
        FlashDeal deal = dealRepository.findByUuid(dealUuid)
                .orElseThrow(() -> new ProductNotFoundException("Deal not found"));
        if (!deal.getSellerUuid().equals(sellerUuid)) {
            throw new RuntimeException("Not the owner of this deal");
        }
        deal.setIsActive(false);
        dealRepository.save(deal);
        return "Deal cancelled";
    }

    private FlashDealResponse mapToResponse(FlashDeal deal, Product product) {
        double discountedPrice = product.getPrice() * (1 - deal.getDiscountPercent() / 100.0);
        return FlashDealResponse.builder()
                .uuid(deal.getUuid())
                .productUuid(product.getUuid())
                .productName(product.getName())
                .originalPrice(product.getPrice())
                .discountPercent(deal.getDiscountPercent())
                .discountedPrice(Math.round(discountedPrice * 100.0) / 100.0)
                .startTime(deal.getStartTime())
                .endTime(deal.getEndTime())
                .isActive(deal.getIsActive())
                .build();
    }
}
