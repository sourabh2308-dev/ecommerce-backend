package com.sourabh.order_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    // Database column mapping
    // @Id - JPA persistence configuration
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Database column mapping
    // @GeneratedValue - JPA persistence configuration
    // Database column mapping
    // @GeneratedValue - JPA persistence configuration
    private Long id;

    private String productUuid;

    private String productName;

    private String productCategory;

    @Column(length = 1000)
    private String productImageUrl;

    private String sellerUuid;

    private Double price;

    private Integer quantity;

    @ManyToOne
    // @ManyToOne applied to field below
    // @ManyToOne applied to field below
    @JoinColumn(name = "order_id")
    // @JoinColumn applied to field below
    // @JoinColumn applied to field below
    private Order order;
}
