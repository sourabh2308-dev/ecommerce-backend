package com.sourabh.order_service.service;

import com.sourabh.order_service.entity.Order;

import java.util.List;

/**
 * Service interface for splitting multi-seller orders into independent sub-orders.
 *
 * <p>When a customer places an order containing products from multiple sellers,
 * this service creates one sub-order per seller so that each seller can fulfil
 * their portion independently. Taxes and discounts are distributed proportionally
 * across the resulting sub-orders.</p>
 *
 * <h3>Order Hierarchy After Splitting</h3>
 * <ul>
 *   <li><strong>MAIN</strong> – the original order retained as a parent reference.</li>
 *   <li><strong>SUB</strong>  – one child order per seller, linked via
 *       {@code parentOrderUuid} and a shared {@code orderGroupId}.</li>
 * </ul>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 */
public interface OrderSplitterService {

    /**
     * Splits a multi-seller order into sub-orders grouped by seller.
     *
     * <p>The main order is marked as {@code MAIN}, assigned a group ID, and saved.
     * For each distinct seller a new {@code SUB} order is created with proportional
     * tax and discount amounts, inheriting shipping and payment details from the parent.</p>
     *
     * @param mainOrder the order to split (must contain items with seller UUIDs)
     * @return list of persisted sub-orders (empty if splitting is not required)
     */
    List<Order> splitOrderBySeller(Order mainOrder);

    /**
     * Determines whether an order contains items from more than one seller.
     *
     * @param order the order to inspect
     * @return {@code true} if the order has items from two or more distinct sellers;
     *         {@code false} otherwise or if the item list is empty/null
     */
    boolean requiresSplitting(Order order);
}
