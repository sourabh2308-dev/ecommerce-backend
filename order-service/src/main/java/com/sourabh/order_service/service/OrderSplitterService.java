package com.sourabh.order_service.service;

import com.sourabh.order_service.entity.Order;

import java.util.List;

public interface OrderSplitterService {

    List<Order> splitOrderBySeller(Order mainOrder);

    boolean requiresSplitting(Order order);
}
