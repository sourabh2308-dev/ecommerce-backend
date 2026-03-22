package com.sourabh.user_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "seller_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerDetail extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String businessName;

    @Column(nullable = false)
    private String businessType;

    @Column(length = 20)
    private String gstNumber;

    @Column(length = 15)
    private String panNumber;

    @Column(nullable = false)
    private String addressLine1;

    private String addressLine2;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false, length = 10)
    private String pincode;

    @Column(nullable = false)
    private String idType;

    @Column(nullable = false)
    private String idNumber;

    @Column(nullable = false)
    private String bankAccountNumber;

    @Column(nullable = false, length = 20)
    private String bankIfscCode;

    @Column(nullable = false)
    private String bankName;

    private LocalDateTime submittedAt;

    private LocalDateTime verifiedAt;
}
