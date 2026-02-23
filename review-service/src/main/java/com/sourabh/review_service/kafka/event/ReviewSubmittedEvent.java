package com.sourabh.review_service.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSubmittedEvent {
    private String productUuid;
    private Integer rating;
    private String reviewUuid;
}
