package com.sourabh.review_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String uuid;

    private String productUuid;

    private String sellerUuid;

    private String buyerUuid;

    @Min(1)
    @Max(5)
    private Integer rating;

    @Column(length = 2000)
    private String comment;

    private LocalDateTime createdAt;
}
