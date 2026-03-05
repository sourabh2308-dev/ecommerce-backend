package com.sourabh.product_service.search.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

/**
 * Elasticsearch document representing an indexed product.
 * <p>
 * Mirrors a subset of the {@code Product} JPA entity fields required for
 * full-text search, filtering, and autocomplete. The document is stored
 * in the {@code products} Elasticsearch index and kept in sync with the
 * PostgreSQL source of truth via {@code ProductSearchService}.
 * </p>
 */
@Document(indexName = "products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDocument {

    /** Product UUID, used as the Elasticsearch document ID. */
    @Id
    private String uuid;

    /** Product name; indexed as full-text for search and autocomplete. */
    @Field(type = FieldType.Text)
    private String name;

    /** Product description; indexed as full-text for search queries. */
    @Field(type = FieldType.Text)
    private String description;

    /** Category name; stored as a keyword for exact-match filtering. */
    @Field(type = FieldType.Keyword)
    private String category;

    /** Product base price; indexed for range-filter queries. */
    @Field(type = FieldType.Double)
    private Double price;

    /** Current stock quantity; informational, included in index. */
    @Field(type = FieldType.Integer)
    private Integer stock;

    /** UUID of the seller who owns the product. */
    @Field(type = FieldType.Keyword)
    private String sellerUuid;

    /** Current lifecycle status (e.g. ACTIVE, DRAFT, BLOCKED). */
    @Field(type = FieldType.Keyword)
    private String status;

    /** Running average review rating (0.0–5.0). */
    @Field(type = FieldType.Double)
    private Double averageRating;

    /** Soft-delete flag; used to filter deleted products from search results. */
    @Field(type = FieldType.Keyword, name = "isDeleted")
    private Boolean isDeleted;

    /** URL of the primary product image. */
    @Field(type = FieldType.Text)
    private String imageUrl;
}
