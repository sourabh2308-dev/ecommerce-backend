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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderSplitterServiceImpl implements OrderSplitterService {

    private final OrderRepository orderRepository;

    @Override
    public List<Order> splitOrderBySeller(Order mainOrder) {
        if (!requiresSplitting(mainOrder)) {
            log.info("Order {} does not require splitting (single seller)", mainOrder.getUuid());
            return Collections.emptyList();
        }

        String groupId = UUID.randomUUID().toString();
        mainOrder.setOrderGroupId(groupId);
        mainOrder.setOrderType(OrderType.MAIN);
        orderRepository.save(mainOrder);

        Map<String, List<OrderItem>> itemsBySeller = mainOrder.getItems().stream()
                .collect(Collectors.groupingBy(OrderItem::getSellerUuid));

        List<Order> subOrders = new ArrayList<>();

        for (Map.Entry<String, List<OrderItem>> entry : itemsBySeller.entrySet()) {
            String sellerUuid = entry.getKey();
            List<OrderItem> sellerItems = entry.getValue();

            double subtotal = sellerItems.stream()
                    .mapToDouble(item -> item.getPrice() * item.getQuantity())
                    .sum();

            double proportionalTaxAmount = (subtotal / mainOrder.getTotalAmount()) * mainOrder.getTaxAmount();

            double proportionalDiscount = 0.0;
            if (mainOrder.getDiscountAmount() != null && mainOrder.getDiscountAmount() > 0) {
                proportionalDiscount = (subtotal / mainOrder.getTotalAmount()) * mainOrder.getDiscountAmount();
            }

            double subOrderTotal = subtotal + proportionalTaxAmount - proportionalDiscount;

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

    @Override
    public boolean requiresSplitting(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return false;
        }

        long distinctSellers = order.getItems().stream()
                .map(OrderItem::getSellerUuid)
                .distinct()
                .count();

        return distinctSellers > 1;
    }
}
