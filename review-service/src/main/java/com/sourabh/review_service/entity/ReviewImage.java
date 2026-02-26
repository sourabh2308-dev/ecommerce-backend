package com.sourabh.review_service.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Image metadata associated with a product review.
 */
@Entity
@Table(name = "review_image")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(nullable = false, length = 1000)
    private String imageUrl;

    @Column(length = 200)
    private String altText;

    private Integer displayOrder;
}
