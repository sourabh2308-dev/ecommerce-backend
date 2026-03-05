package com.sourabh.review_service.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity representing an image attached to a {@link Review}.
 *
 * <p>Buyers may upload up to five images per review. Each image stores a
 * publicly accessible URL, optional alt-text for accessibility, and a
 * display order for consistent rendering.
 *
 * <h3>Table</h3>
 * Mapped to the {@code review_image} table in PostgreSQL.
 *
 * @see Review#getImages()
 * @see com.sourabh.review_service.repository.ReviewImageRepository
 */
@Entity
@Table(name = "review_image")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewImage {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The parent review this image belongs to.
     * Lazily fetched to avoid unnecessary joins when only
     * image metadata is needed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    /** Publicly accessible URL pointing to the image resource. */
    @Column(nullable = false, length = 1000)
    private String imageUrl;

    /** Optional alt text for accessibility / SEO purposes. */
    @Column(length = 200)
    private String altText;

    /** Position index controlling the rendering order of images. */
    private Integer displayOrder;
}
