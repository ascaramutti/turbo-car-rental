package com.turbo.review.service;

import com.turbo.review.dto.ReviewResponse;
import com.turbo.review.service.command.CreateReviewCommand;

import java.util.List;

public interface ReviewService {

    ReviewResponse createReview(CreateReviewCommand command);

    List<ReviewResponse> getReviewsForUser(Long userId);

    List<ReviewResponse> getReviewsForBooking(Long bookingId);
}
