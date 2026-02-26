package com.sourabh.order_service.service;

import com.sourabh.order_service.entity.Order;

import java.util.List;

/**
 * Handles splitting of multi-seller orders into sub-orders.
 */
public interface OrderSplitterService {

    /**
     * Splits a multi-seller order into sub-orders, one per seller.
     * Updates the main order to be a parent and creates child sub-orders.
     * 
     * @param mainOrder The main order containing items from multiple sellers
     * @return List of created sub-orders (excludes the main order)
     */
    List<Order> splitOrderBySeller(Order mainOrder);

    /**
     * Checks if an order contains items from multiple sellers.
     * 
     * @param order The order to check
     * @return true if order has items from 2+ sellers
     */
    boolean requiresSplitting(Order order);
}
