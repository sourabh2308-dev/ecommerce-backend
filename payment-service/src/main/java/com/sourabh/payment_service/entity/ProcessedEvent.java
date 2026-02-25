package com.sourabh.payment_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tracks processed Kafka event IDs to provide idempotent event consumption.
 */
@Entity
@Table(name = "processed_events", indexes = {
        @Index(name = "idx_pe_event_id", columnList = "event_id", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    @Id
    // Database column mapping
    // @Id - JPA persistence configuration
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Database column mapping
    // @GeneratedValue - JPA persistence configuration
    // Database column mapping
    // @GeneratedValue - JPA persistence configuration
    private Long id;

    /**


     * DATABASE COLUMN MAPPING


     * 


     * @Column configures how this field maps to database column:


     * - name: Actual column name in table (default: field name in snake_case)


     * - nullable: Can be NULL in database (default: true)


     * - unique: Enforces uniqueness constraint (default: false)


     * - length: Max length for VARCHAR columns (default: 255)


     * - updatable: Can be modified after insert (default: true)


     * - insertable: Included in INSERT statements (default: true)


     * 


     * JPA auto-generates SQL schema based on these annotations.


     */


    @Column(name = "event_id", nullable = false, unique = true, length = 64)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private String eventId;

    @Column(name = "topic", nullable = false, length = 128)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private String topic;

    @Column(name = "processed_at", nullable = false)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private LocalDateTime processedAt;
}
