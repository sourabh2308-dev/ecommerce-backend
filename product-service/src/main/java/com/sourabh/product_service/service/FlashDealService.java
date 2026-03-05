package com.sourabh.product_service.service;

import com.sourabh.product_service.dto.request.FlashDealRequest;
import com.sourabh.product_service.dto.response.FlashDealResponse;

import java.util.List;

/**
 * Service interface for managing time-limited flash deals on products.
 *
 * <p>Flash deals apply a temporary discount percentage to a product within
 * a defined start/end time window. Only the product owner (seller) may
 * create or cancel deals. A scheduled task expires deals whose end time
 * has passed.</p>
 *
 * @see com.sourabh.product_service.service.impl.FlashDealServiceImpl
 * @see com.sourabh.product_service.entity.FlashDeal
 */
public interface FlashDealService {

    /**
     * Creates a new flash deal for a product owned by the seller.
     *
     * @param sellerUuid the UUID of the seller creating the deal
     * @param request    the deal details (product UUID, discount %, start/end times)
     * @return the created {@link FlashDealResponse}
     * @throws com.sourabh.product_service.exception.ProductNotFoundException
     *         if the product does not exist
     * @throws RuntimeException         if the seller does not own the product
     * @throws IllegalArgumentException if the end time precedes the start time
     */
    FlashDealResponse createDeal(String sellerUuid, FlashDealRequest request);

    /**
     * Returns all currently active flash deals (current time falls within
     * each deal's start/end window and the deal has not been cancelled).
     *
     * @return list of active {@link FlashDealResponse} entries
     */
    List<FlashDealResponse> getActiveDeals();

    /**
     * Returns all flash deals created by a specific seller,
     * ordered by creation date descending.
     *
     * @param sellerUuid the UUID of the seller
     * @return list of the seller's deals
     */
    List<FlashDealResponse> getMyDeals(String sellerUuid);

    /**
     * Cancels (deactivates) a flash deal owned by the seller.
     *
     * @param dealUuid   the UUID of the deal to cancel
     * @param sellerUuid the UUID of the requesting seller
     * @return confirmation message
     * @throws com.sourabh.product_service.exception.ProductNotFoundException
     *         if the deal is not found
     * @throws RuntimeException if the seller does not own the deal
     */
    String cancelDeal(String dealUuid, String sellerUuid);
}
