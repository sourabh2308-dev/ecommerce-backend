package com.sourabh.review_service.controller;

import com.sourabh.review_service.entity.Review;
import com.sourabh.review_service.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
@Slf4j
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
        String buyerUuid = request.getHeader("X-User-UUID");

        return ResponseEntity.ok(
                reviewService.createReview(orderUuid, productUuid, rating, comment, role, buyerUuid)
        );
    }

    @GetMapping("/product/{productUuid}")
    public ResponseEntity<List<Review>> getReviewsByProduct(
            @PathVariable String productUuid) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productUuid));
    }
}

