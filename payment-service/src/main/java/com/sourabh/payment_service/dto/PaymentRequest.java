package com.sourabh.payment_service.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * PAYMENT REQUEST DTO
 * 
 * Data Transfer Object for payment initiation requests.
 * Contains order and payment details for processing.
 * 
 * USAGE:
 * - Sent by buyer to initiate payment for an order
 * - Validated before payment processing
 * - Used in Kafka event publishing
 */
@Getter
@Setter
public class PaymentRequest {

    // UUID of the order being paid for
    // Links payment to specific order
    private String orderUuid;
    
    // Total payment amount (item prices + delivery fees + platform commission)
    // Calculated by order-service and sent in event
    private Double amount;
    
    // UUID of the buyer making the payment
    // Used for authorization and audit trail
    private String buyerUuid;
}
