package com.sourabh.order_service.kafka.event;

public record OrderCreatedOutboxRequestedEvent(Long outboxId) {
}