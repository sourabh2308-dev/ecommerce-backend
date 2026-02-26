package com.sourabh.order_service.service.impl;

import com.sourabh.order_service.entity.Order;
import com.sourabh.order_service.entity.OrderItem;
import com.sourabh.order_service.entity.OrderStatus;
import com.sourabh.order_service.entity.OrderType;
import com.sourabh.order_service.entity.PaymentStatus;
import com.sourabh.order_service.repository.OrderRepository;
import com.sourabh.order_service.service.OrderSplitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ══════════════════════════════════════════════════════════════════════════════
 * ORDER SPLITTER SERVICE IMPLEMENTATION
 * ══════════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * --------
 * Handles automatic order splitting for multi-seller purchase scenarios.
 * This service orchestrates:
 *   1. Detection of multi-seller orders (items from multiple sellers)
 *   2. Creation of sub-orders grouped by seller
 *   3. Fair distribution of taxes and discounts across sub-orders
 *   4. Maintenance of order relationships (parent->children links)
 *   5. Proportional calculation of charges (tax, discount per sub-order)
 * 
 * KEY RESPONSIBILITIES:
 * ---------------------
 * - Detect when order requires splitting (>1 seller)
 * - Create parent-child order relationship
 * - Group items by seller UUID
 * - Calculate proportional tax and discount for each sub-order
 * - Assign unique UUID to each sub-order
 * - Link all sub-orders with group ID for coordinated processing
 * - Inherit shipping and order metadata from parent order
 * - Update order statuses correctly (parent=MAIN, children=SUB)
 * 
 * ORDER STRUCTURE AFTER SPLITTING:
 * ─────────────────────────
 * 
 * PARENT ORDER (MAIN):
 *   - Order UUID: Original UUID from customer
 *   - Order Type: MAIN (indicates it's the parent)
 *   - Items: All items (not reassigned)
 *   - Status: CREATED (mirrors original)
 *   - Total: Original total (before splitting)
 *   - Group ID: Unique UUID linking all sub-orders
 * 
 * SUB-ORDERS (per seller):
 *   - Order UUID: New unique UUID
 *   - Parent Order UUID: Reference back to main order
 *   - Items: Only items for this seller
 *   - Order Type: SUB (indicates it's a child)
 *   - Total: Proportional subtotal + proportional tax - proportional discount
 *   - Group ID: Same as parent (for coordination)
 *   - Status: CREATED (inherited from parent)
 *   - Payment Status: Inherited from parent
 * 
 * EXAMPLE SCENARIO:
 * ──────────────
 * Customer buys:
 *   - 2x Product A from Seller1 ($100)
 *   - 1x Product B from Seller2 ($50)
 *   - Coupon discount: $10
 *   - Tax (10%): $15
 *   - Total: $155
 * 
 * After splitting:
 *   Parent Order (MAIN):
 *     UUID: <original>
 *     Items: A, A, B
 *     Total: $155
 *     Group ID: <group-uuid>
 *   
 *   Sub-Order 1 (Seller1):
 *     UUID: <new-uuid-1>
 *     Items: A, A (from this seller)
 *     Subtotal: $100
 *     Proportional tax: $10 (100/150 * 15)
 *     Proportional discount: -$6.67 (100/150 * 10)
 *     Total: $103.33
 *     Parent UUID: <original>
 *     Group ID: <group-uuid>
 *   
 *   Sub-Order 2 (Seller2):
 *     UUID: <new-uuid-2>
 *     Items: B (from this seller)
 *     Subtotal: $50
 *     Proportional tax: $5 (50/150 * 15)
 *     Proportional discount: -$3.33 (50/150 * 10)
 *     Total: $51.67
 *     Parent UUID: <original>
 *     Group ID: <group-uuid>
 * 
 * FULFILLMENT WORKFLOW:
 * ─────────────────
 * 1. Customer places order with items from multiple sellers
 * 2. OrderService.createOrder() detects multi-seller scenario
 * 3. Calls OrderSplitterService.splitOrderBySeller()
 * 4. Service creates parent order + sub-orders in database
 * 5. Each sub-order assigned to respective seller for fulfillment
 * 6. Seller fulfills their sub-order independently
 * 7. Shipping notifications sent per sub-order
 * 8. When all sub-orders delivered, parent order marked complete
 * 
 * DEPENDENCIES:
 * ──────────────
 * - OrderRepository: Saves parent and sub-orders to database
 * 
 * ANNOTATIONS:
 * ─────────────
 * @Service: Marks class as Spring service layer component
 * @RequiredArgsConstructor: Lombok generates constructor for final fields
 * @Slf4j (Lombok): Creates 'log' field for logging split operations
 * @Transactional: All methods are transactional (atomic database operations)
 * 
 * ══════════════════════════════════════════════════════════════════════════════
 * 
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 */
/**
 * Implementation of order splitting logic for multi-seller scenarios.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderSplitterServiceImpl implements OrderSplitterService {

    private final OrderRepository orderRepository;

    /**
     * Split an order into multiple sub-orders grouped by seller.
     * 
     * PURPOSE:
     * Atomically splits a multi-seller order into sub-orders for independent fulfillment.
     * Each seller receives their own sub-order containing only their items.
     * Taxes and discounts are proportionally distributed.
     * 
     * PREREQUISITES:
     * - Order must not be null
     * - Order must have items (cannot split empty order)
     * - Items must have sellerUuid values set
     * - Order must have calculated totalAmount, taxAmount, etc.
     * 
     * PROCESS FLOW:
     * 
     * 1. CHECK: Call requiresSplitting() to validate order needs splitting
     *    - Returns false if single seller or no items -> returns empty list
     *    - Returns true if multiple sellers present -> proceed with split
     * 
     * 2. SETUP: Create unique order group ID (UUID)
     *    - Link main order with group ID
     *    - Mark main order type as MAIN
     *    - Save main order to database
     * 
     * 3. GROUP: Partition items by seller using stream groupingBy
     *    - Key: sellerUuid
     *    - Value: List of items for that seller
     * 
     * 4. ITERATE: For each seller, create a sub-order:
     * 
     *    a. Calculate subtotal: Sum(item.price * item.quantity) for seller's items
     * 
     *    b. Calculate proportional tax:
     *       formula: (sellerSubtotal / mainOrderTotal) * mainOrderTaxAmount
     *       Example: Seller items = $100, total = $200, tax = $20
     *       -> proportional tax = (100/200) * 20 = $10
     * 
     *    c. Calculate proportional discount (if coupon applied):
     *       formula: (sellerSubtotal / mainOrderTotal) * mainOrderDiscountAmount
     *       Example: Seller items = $100, total = $200, discount = $20
     *       -> proportional discount = (100/200) * 20 = $10
     * 
     *    d. Calculate sub-order total:
     *       formula: subtotal + proportionalTax - proportionalDiscount
     *       Example: $100 + $10 - $10 = $100
     * 
     *    e. Build sub-order entity:
     *       - Generate new UUID for sub-order
     *       - Set parentOrderUuid = main order UUID (for hierarchy)
     *       - Set orderGroupId = group UUID (for coordinated processing)
     *       - Set orderType = SUB (marks as child order)
     *       - Inherit shipping details from main order
     *       - Inherit payment status from main order
     *       - Set calculated amounts (tax, discount, total)
     * 
     *    f. Reassign items to sub-order:
     *       - Set order reference on each item entity
     *       - Associate items list with sub-order
     * 
     *    g. Persist sub-order and items:
     *       - Save to database in single transaction
     *       - Logging for audit trail
     * 
     * 5. LOG: Save success information
     *    - Log parent order UUID, count of sub-orders, group ID
     *    - Log details of each sub-order created
     * 
     * 6. RETURN: Return list of created sub-orders
     * 
     * TRANSACTION SAFETY:
     * @Transactional ensures all database operations are atomic.
     * If any step fails, entire transaction rolls back (no partial splits).
     * 
     * LOGGING:
     * All operations logged at INFO level for visibility in logs:
     * - Skip message if no splitting needed
     * - Creation message for each sub-order with seller UUID, item count, total
     * - Final summary with parent UUID, sub-order count, group ID
     * 
     * @param mainOrder Order to split (must have items and seller info)
     * 
     * @return List of created sub-orders (empty if single seller or no items).
     *         Each sub-order is persisted to database with:
     *         - Unique UUID
     *         - All items for that seller
     *         - Proportional taxes and discounts
     *         - Parent and group references
     *         - Inherited shipping and payment info
     * 
     * @throws RuntimeException if database save fails
     * 
     * @see #requiresSplitting(Order) for splitting eligibility check
     */
    @Override
    public List<Order> splitOrderBySeller(Order mainOrder) {
        if (!requiresSplitting(mainOrder)) {
            log.info("Order {} does not require splitting (single seller)", mainOrder.getUuid());
            return Collections.emptyList();
        }

        // Generate a group ID to link all related orders
        String groupId = UUID.randomUUID().toString();
        mainOrder.setOrderGroupId(groupId);
        mainOrder.setOrderType(OrderType.MAIN);  // Mark as parent
        orderRepository.save(mainOrder);

        // Group items by seller
        Map<String, List<OrderItem>> itemsBySeller = mainOrder.getItems().stream()
                .collect(Collectors.groupingBy(OrderItem::getSellerUuid));

        List<Order> subOrders = new ArrayList<>();

        for (Map.Entry<String, List<OrderItem>> entry : itemsBySeller.entrySet()) {
            String sellerUuid = entry.getKey();
            List<OrderItem> sellerItems = entry.getValue();

            // Calculate totals for this sub-order
            double subtotal = sellerItems.stream()
                    .mapToDouble(item -> item.getPrice() * item.getQuantity())
                    .sum();
            
            // Proportional tax based on subtotal
            double proportionalTaxAmount = (subtotal / mainOrder.getTotalAmount()) * mainOrder.getTaxAmount();
            
            // Proportional discount if coupon applied
            double proportionalDiscount = 0.0;
            if (mainOrder.getDiscountAmount() != null && mainOrder.getDiscountAmount() > 0) {
                proportionalDiscount = (subtotal / mainOrder.getTotalAmount()) * mainOrder.getDiscountAmount();
            }

            double subOrderTotal = subtotal + proportionalTaxAmount - proportionalDiscount;

            // Create sub-order
            Order subOrder = Order.builder()
                    .uuid(UUID.randomUUID().toString())
                    .buyerUuid(mainOrder.getBuyerUuid())
                    .totalAmount(subOrderTotal)
                    .status(OrderStatus.CREATED)
                    .paymentStatus(mainOrder.getPaymentStatus())  // Inherit payment status from main
                    .orderType(OrderType.SUB)
                    .parentOrderUuid(mainOrder.getUuid())
                    .orderGroupId(groupId)
                    .shippingName(mainOrder.getShippingName())
                    .shippingAddress(mainOrder.getShippingAddress())
                    .shippingCity(mainOrder.getShippingCity())
                    .shippingState(mainOrder.getShippingState())
                    .shippingPincode(mainOrder.getShippingPincode())
                    .shippingPhone(mainOrder.getShippingPhone())
                    .taxPercent(mainOrder.getTaxPercent())
                    .taxAmount(proportionalTaxAmount)
                    .currency(mainOrder.getCurrency())
                    .couponCode(mainOrder.getCouponCode())
                    .discountAmount(proportionalDiscount)
                    .isDeleted(false)
                    .build();

            // Associate items with sub-order
            for (OrderItem item : sellerItems) {
                item.setOrder(subOrder);
            }
            subOrder.setItems(sellerItems);

            orderRepository.save(subOrder);
            subOrders.add(subOrder);

            log.info("Created sub-order {} for seller {} with {} items (total: {})", 
                    subOrder.getUuid(), sellerUuid, sellerItems.size(), subOrderTotal);
        }

        log.info("Split order {} into {} sub-orders (group: {})", 
                mainOrder.getUuid(), subOrders.size(), groupId);

        return subOrders;
    }

    /**
     * Check if an order requires splitting due to multiple sellers.
     * 
     * PURPOSE:
     * Determines if an order needs to be split into sub-orders based on seller count.
     * Quick eligibility check before performing expensive splitting operation.
     * 
     * CONDITIONS FOR SPLITTING:
     * - Order must have items (empty items = no split)
     * - Order must have > 1 distinct seller (single seller = no split)
     * 
     * PROCESS:
     * 1. Handle edge case: Return false if items are null or empty
     * 2. Stream over order items and extract sellerUuid from each
     * 3. Count distinct seller UUIDs
     * 4. Return true if count > 1 (multiple sellers)
     * 5. Return false if count <= 1 (single or no seller)
     * 
     * EFFICIENCY:
     * Uses streams with distinct().count() which:
     * - Iterates through items once
     * - Only counts distinct values (early termination at count=2)
     * - Lazy evaluation (stops as soon as multiple sellers detected)
     * 
     * @param order Order to check for splitting requirement
     * 
     * @return true if order contains items from multiple sellers and needs splitting,
     *         false if single seller or no items
     */
    @Override
    public boolean requiresSplitting(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return false;
        }

        // Get distinct seller UUIDs
        long distinctSellers = order.getItems().stream()
                .map(OrderItem::getSellerUuid)
                .distinct()
                .count();

        return distinctSellers > 1;
    }
}
