package com.sourabh.review_service.controller;

import com.sourabh.review_service.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<String> createReview(
            @RequestParam String orderUuid,
            @RequestParam String productUuid,
            @RequestParam Integer rating,
            @RequestParam(required = false) String comment,
            HttpServletRequest request) {

        String role = request.getHeader("X-User-Role");
        String buyerUuid =
                request.getHeader("X-User-UUID");

        return ResponseEntity.ok(
                reviewService.createReview(
                        orderUuid,
                        productUuid,
                        rating,
                        comment,
                        role,
                        buyerUuid
                )
        );
    }
}

