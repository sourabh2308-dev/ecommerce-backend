package com.sourabh.order_service.entity;

/**
 * Distinguishes between main orders (placed by buyers) and sub-orders (split by seller).
 */
public enum OrderType {
    /** Single order or parent of multi-seller order */
    MAIN,
    
    /** Sub-order created from splitting a multi-seller order */
    SUB
}
