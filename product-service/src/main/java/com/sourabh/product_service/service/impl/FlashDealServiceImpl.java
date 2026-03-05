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

/**
 * Implementation of {@link FlashDealService} for time-limited product discounts.
 *
 * <p>Creates, retrieves, and cancels flash deals. Each deal records a
 * discount percentage, a validity window (start/end time), and the owning
 * seller. A scheduled task (configured via {@code scheduler.expire-flash-deals.cron})
 * automatically deactivates expired deals.</p>
 *
 * @see FlashDealService
 * @see FlashDealRepository
 */
@Service
@RequiredArgsConstructor
public class FlashDealServiceImpl implements FlashDealService {

    /** Repository for flash-deal persistence. */
    private final FlashDealRepository dealRepository;

    /** Repository for product lookups (ownership validation). */
    private final ProductRepository productRepository;

    /**
     * {@inheritDoc}
     *
     * <p>Validates product ownership, ensures the time window is valid,
     * then persists a new {@link FlashDeal} entity.</p>
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>Queries for deals whose time window contains the current
     * timestamp and that have not been cancelled.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public List<FlashDealResponse> getActiveDeals() {
        return dealRepository.findAllActive(LocalDateTime.now()).stream()
                .map(d -> mapToResponse(d, d.getProduct()))
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<FlashDealResponse> getMyDeals(String sellerUuid) {
        return dealRepository.findBySellerUuidOrderByCreatedAtDesc(sellerUuid).stream()
                .map(d -> mapToResponse(d, d.getProduct()))
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sets the deal's {@code isActive} flag to {@code false}. Only the
     * deal's owning seller is authorised to perform cancellation.</p>
     */
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

    /**
     * Converts a {@link FlashDeal} entity and its associated {@link Product}
     * into a {@link FlashDealResponse} DTO, computing the discounted price.
     *
     * @param deal    the flash-deal entity
     * @param product the product associated with the deal
     * @return the populated response DTO
     */
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
