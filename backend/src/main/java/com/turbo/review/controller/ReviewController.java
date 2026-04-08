package com.turbo.review.controller;

import com.turbo.config.SecurityHelper;
import com.turbo.review.controller.mapper.ReviewControllerMapper;
import com.turbo.review.dto.CreateReviewRequest;
import com.turbo.review.dto.ReviewResponse;
import com.turbo.review.service.ReviewService;
import com.turbo.user.model.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewControllerMapper controllerMapper;
    private final SecurityHelper securityHelper;

    @PostMapping("/bookings/{bookingId}/reviews")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable @Positive Long bookingId,
            @Valid @RequestBody CreateReviewRequest request) {
        User user = securityHelper.getCurrentUser();
        request.setBookingId(bookingId);
        ReviewResponse response = reviewService.createReview(
                controllerMapper.toCreateReviewCommand(request, user.getUserId()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bookings/{bookingId}/reviews")
    public ResponseEntity<List<ReviewResponse>> getBookingReviews(
            @PathVariable @Positive Long bookingId) {
        List<ReviewResponse> reviews = reviewService.getReviewsForBooking(bookingId);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/users/{userId}/reviews")
    public ResponseEntity<List<ReviewResponse>> getUserReviews(
            @PathVariable @Positive Long userId) {
        List<ReviewResponse> reviews = reviewService.getReviewsForUser(userId);
        return ResponseEntity.ok(reviews);
    }
}
