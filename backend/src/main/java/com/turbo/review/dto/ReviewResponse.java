package com.turbo.review.dto;

import com.turbo.review.model.enums.ReviewType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReviewResponse {

    private Long reviewId;
    private Long bookingId;
    private Long vehicleId;
    private String vehicleSummary;
    private Long reviewerUserId;
    private String reviewerName;
    private Long revieweeUserId;
    private String revieweeName;
    private ReviewType reviewType;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
