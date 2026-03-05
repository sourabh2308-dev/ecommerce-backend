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
 * Kafka consumer that reacts to order-status-change events published by
 * the order-service on the {@code order.status.changed} topic.
 * <p>
 * For every qualifying event the consumer:
 * <ol>
 *   <li>Creates an in-app {@link com.sourabh.user_service.entity.Notification}
 *       via {@link NotificationService}.</li>
 *   <li>Sends an HTML email to the buyer via {@link EmailService}.</li>
 *   <li>On {@code DELIVERED} status, fetches the invoice PDF from order-service
 *       and emails it as an attachment.</li>
 * </ol>
 * </p>
 *
 * @see OrderStatusChangedEvent
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    /** Service used to persist in-app notifications. */
    private final NotificationService notificationService;

    /** Service responsible for sending transactional emails. */
    private final EmailService emailService;

    /** Repository for looking up buyer details by UUID. */
    private final UserRepository userRepository;

    /** Feign client to fetch invoice PDFs from order-service. */
    private final com.sourabh.user_service.feign.OrderServiceClient orderServiceClient;

    /** Shared secret used when calling internal order-service endpoints. */
    @Value("${internal.secret}")
    private String internalSecret;

    /**
     * Static mapping from order-status strings to the corresponding
     * {@link NotificationType} enum value.
     */
    private static final Map<String, NotificationType> STATUS_TO_TYPE = Map.of(
            "CONFIRMED", NotificationType.ORDER_CONFIRMED,
            "SHIPPED", NotificationType.ORDER_SHIPPED,
            "DELIVERED", NotificationType.ORDER_DELIVERED,
            "CANCELLED", NotificationType.ORDER_CANCELLED
    );

    /**
     * Handles incoming {@link OrderStatusChangedEvent} messages from the
     * {@code order.status.changed} Kafka topic.
     * <p>
     * Determines the appropriate notification type, builds a title and
     * message, creates an in-app notification, and dispatches an email.
     * </p>
     *
     * @param event the deserialized order-status-change event
     */
    @KafkaListener(topics = "order.status.changed", groupId = "user-service-group")
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Received OrderStatusChangedEvent: orderUuid={}, {} -> {}",
                event.getOrderUuid(), event.getOldStatus(), event.getNewStatus());

        try {
            NotificationType type = STATUS_TO_TYPE.getOrDefault(
                    event.getNewStatus(), NotificationType.SYSTEM);

            String title = buildTitle(event.getNewStatus(), event.getOrderUuid());
            String message = buildMessage(event);

            notificationService.sendNotification(
                    event.getBuyerUuid(), type, title, message, event.getOrderUuid());

            sendEmailNotification(event, title, message);

        } catch (Exception e) {
            log.error("Failed to process order status event for order {}: {}",
                    event.getOrderUuid(), e.getMessage(), e);
        }
    }

    /**
     * Sends an HTML email to the buyer for the given order event.
     * <p>
     * If the new status is {@code DELIVERED}, a follow-up email with the
     * invoice PDF attachment is also sent.
     * </p>
     *
     * @param event   the order-status-change event
     * @param title   the email subject / notification title
     * @param message the human-readable notification body
     */
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

    /**
     * Builds a user-facing notification title based on the new order status.
     *
     * @param newStatus the new order status string
     * @param orderUuid the order's public UUID
     * @return a formatted title string
     */
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

    /**
     * Builds a descriptive notification message based on the event's new status.
     *
     * @param event the order-status-change event
     * @return a human-readable message body
     */
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
