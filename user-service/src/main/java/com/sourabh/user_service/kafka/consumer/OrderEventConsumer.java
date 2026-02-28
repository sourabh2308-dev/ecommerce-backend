package com.sourabh.user_service.kafka.consumer;

import com.sourabh.user_service.entity.NotificationType;
import com.sourabh.user_service.kafka.event.OrderStatusChangedEvent;
import com.sourabh.user_service.service.EmailService;
import com.sourabh.user_service.service.NotificationService;
import com.sourabh.user_service.entity.User;
import com.sourabh.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.Optional;

/**
 * Consumes order status change events from order-service.
 * Creates in-app notifications and sends email notifications.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final NotificationService notificationService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final com.sourabh.user_service.feign.OrderServiceClient orderServiceClient;

    @Value("${internal.secret}")
    private String internalSecret;

    private static final Map<String, NotificationType> STATUS_TO_TYPE = Map.of(
            "CONFIRMED", NotificationType.ORDER_CONFIRMED,
            "SHIPPED", NotificationType.ORDER_SHIPPED,
            "DELIVERED", NotificationType.ORDER_DELIVERED,
            "CANCELLED", NotificationType.ORDER_CANCELLED
    );

    @KafkaListener(topics = "order.status.changed", groupId = "user-service-group")
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Received OrderStatusChangedEvent: orderUuid={}, {} -> {}",
                event.getOrderUuid(), event.getOldStatus(), event.getNewStatus());

        try {
            // Determine notification type
            NotificationType type = STATUS_TO_TYPE.getOrDefault(
                    event.getNewStatus(), NotificationType.SYSTEM);

            String title = buildTitle(event.getNewStatus(), event.getOrderUuid());
            String message = buildMessage(event);

            // Create in-app notification
            notificationService.sendNotification(
                    event.getBuyerUuid(), type, title, message, event.getOrderUuid());

            // Send email
            sendEmailNotification(event, title, message);

        } catch (Exception e) {
            log.error("Failed to process order status event for order {}: {}",
                    event.getOrderUuid(), e.getMessage(), e);
        }
    }

    private void sendEmailNotification(OrderStatusChangedEvent event, String title, String message) {
        Optional<User> userOpt = userRepository.findByUuidAndIsDeletedFalse(event.getBuyerUuid());
        if (userOpt.isEmpty()) {
            log.warn("User not found for email notification: {}", event.getBuyerUuid());
            return;
        }
        User user = userOpt.get();
        try {
            String htmlBody = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                      <h2 style="color: #4F46E5;">%s</h2>
                      <p>Hi %s,</p>
                      <p>%s</p>
                      <p style="margin-top: 20px;">
                        <strong>Order ID:</strong> %s<br/>
                        <strong>Amount:</strong> %s %.2f
                      </p>
                      <hr style="border: 1px solid #e5e7eb;"/>
                      <p style="color: #6b7280; font-size: 12px;">
                        This is an automated notification from E-Commerce Platform.
                      </p>
                    </div>
                    """.formatted(
                    title,
                    user.getFirstName(),
                    message,
                    event.getOrderUuid(),
                    event.getCurrency() != null ? event.getCurrency() : "INR",
                    event.getTotalAmount() != null ? event.getTotalAmount() : 0.0
            );
            emailService.sendHtmlEmail(user.getEmail(), title, htmlBody);
            log.info("Order status email sent to {}", user.getEmail());

            // when delivered, automatically send the invoice attachment
            if ("DELIVERED".equals(event.getNewStatus())) {
                try {
                    byte[] pdf = orderServiceClient.getInvoice(event.getOrderUuid(), internalSecret);
                    String base64 = java.util.Base64.getEncoder().encodeToString(pdf);
                    emailService.sendInvoiceEmail(user.getEmail(), event.getOrderUuid(), base64);
                    log.info("Invoice attached email sent to {}", user.getEmail());
                } catch (Exception ex) {
                    log.error("Failed to fetch/send invoice for {}: {}", event.getOrderUuid(), ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to send order email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private String buildTitle(String newStatus, String orderUuid) {
        return switch (newStatus) {
            case "CONFIRMED" -> "Order Confirmed - " + orderUuid;
            case "SHIPPED" -> "Your Order Has Been Shipped!";
            case "DELIVERED" -> "Order Delivered Successfully!";
            case "CANCELLED" -> "Order Cancelled - " + orderUuid;
            case "REFUND_ISSUED" -> "Refund Processed for Order " + orderUuid;
            default -> "Order Update - " + orderUuid;
        };
    }

    private String buildMessage(OrderStatusChangedEvent event) {
        return switch (event.getNewStatus()) {
            case "CONFIRMED" -> "Your order has been confirmed and is being prepared.";
            case "SHIPPED" -> "Your order is on its way! Track your shipment for delivery updates.";
            case "DELIVERED" -> "Your order has been delivered. We hope you enjoy your purchase!";
            case "CANCELLED" -> "Your order has been cancelled. If you didn't request this, please contact support.";
            case "REFUND_ISSUED" -> "A refund has been processed for your order. It may take 5-7 business days.";
            default -> "Your order status has been updated to: " + event.getNewStatus();
        };
    }
}
