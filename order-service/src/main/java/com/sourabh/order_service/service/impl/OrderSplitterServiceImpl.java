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
 * Implementation of {@link OrderSplitterService} that splits multi-seller orders
 * into independent sub-orders for parallel fulfilment.
 *
 * <p>When a customer places an order with items from more than one seller, this
 * service creates a parent–child hierarchy:</p>
 * <ul>
 *   <li><strong>MAIN order</strong> – the original order, re-tagged with
 *       {@code OrderType.MAIN} and a shared {@code orderGroupId}.</li>
 *   <li><strong>SUB orders</strong> – one per seller, each containing only that
 *       seller’s items and proportional shares of tax and discount.</li>
 * </ul>
 *
 * <p>All database writes are executed within a single {@code @Transactional}
 * boundary to guarantee atomicity (either all sub-orders are created or none).</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderSplitterServiceImpl implements OrderSplitterService {

    /** Repository for persisting parent and sub-order entities. */
    private final OrderRepository orderRepository;

    /**
     * {@inheritDoc}
     *
     * <p>Assigns a shared {@code orderGroupId} to the parent, groups items by
     * seller UUID, then creates one {@code SUB} order per seller with
     * proportional tax and discount amounts. All sub-orders share shipping
     * details and payment status with the parent.</p>
     *
     * @param mainOrder the order to split
     * @return list of persisted sub-orders; empty if no split is needed
     */
    @Override
    public List<Order> splitOrderBySeller(Order mainOrder) {
        if (!requiresSplitting(mainOrder)) {
            log.info("Order {} does not require splitting (single seller)", mainOrder.getUuid());
            return Collections.emptyList();
        }

        // Generate a shared group ID linking parent and all sub-orders
        String groupId = UUID.randomUUID().toString();
        mainOrder.setOrderGroupId(groupId);
        mainOrder.setOrderType(OrderType.MAIN);
        orderRepository.save(mainOrder);

        // Partition items by seller UUID
        Map<String, List<OrderItem>> itemsBySeller = mainOrder.getItems().stream()
                .collect(Collectors.groupingBy(OrderItem::getSellerUuid));

        List<Order> subOrders = new ArrayList<>();

        for (Map.Entry<String, List<OrderItem>> entry : itemsBySeller.entrySet()) {
            String sellerUuid = entry.getKey();
            List<OrderItem> sellerItems = entry.getValue();

            // Seller subtotal = sum of (price * qty) for this seller's items
            double subtotal = sellerItems.stream()
                    .mapToDouble(item -> item.getPrice() * item.getQuantity())
                    .sum();
            
            // Proportional tax = (seller subtotal / order total) * total tax
            double proportionalTaxAmount = (subtotal / mainOrder.getTotalAmount()) * mainOrder.getTaxAmount();
            
            // Proportional discount = (seller subtotal / order total) * total discount
            double proportionalDiscount = 0.0;
            if (mainOrder.getDiscountAmount() != null && mainOrder.getDiscountAmount() > 0) {
                proportionalDiscount = (subtotal / mainOrder.getTotalAmount()) * mainOrder.getDiscountAmount();
            }

            double subOrderTotal = subtotal + proportionalTaxAmount - proportionalDiscount;

            // Build sub-order with inherited shipping and payment details
            Order subOrder = Order.builder()
                    .uuid(UUID.randomUUID().toString())
                    .buyerUuid(mainOrder.getBuyerUuid())
                    .totalAmount(subOrderTotal)
                    .status(OrderStatus.CREATED)
                    .paymentStatus(mainOrder.getPaymentStatus())
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

            // Re-associate items with sub-order entity
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
     * {@inheritDoc}
     *
     * <p>Counts distinct {@code sellerUuid} values across the order’s items.
     * Returns {@code true} when two or more distinct sellers are found.</p>
     *
     * @param order the order to inspect
     * @return {@code true} if splitting is required
     */
    @Override
    public boolean requiresSplitting(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return false;
        }

        // Count distinct seller UUIDs across all items
        long distinctSellers = order.getItems().stream()
                .map(OrderItem::getSellerUuid)
                .distinct()
                .count();

        return distinctSellers > 1;
    }
}
