package com.sourabh.review_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateReviewRequest {

    /** Only the comment can be updated after submission. */
    private String comment;
}
