package com.turbo.review.service.mapper;

import com.turbo.booking.model.Booking;
import com.turbo.review.dto.ReviewResponse;
import com.turbo.review.model.Review;
import com.turbo.review.model.enums.ReviewType;
import com.turbo.review.service.command.CreateReviewCommand;
import com.turbo.user.model.User;
import com.turbo.vehicle.model.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewServiceMapper {

    // ── Entity creation ─────────────────────────────────────────────

    @Mapping(source = "command.rating", target = "rating")
    @Mapping(source = "command.comment", target = "comment")
    @Mapping(source = "booking", target = "booking")
    @Mapping(source = "reviewer", target = "reviewer")
    @Mapping(source = "reviewee", target = "reviewee")
    @Mapping(source = "vehicle", target = "vehicle")
    @Mapping(source = "reviewType", target = "reviewType")
    @Mapping(target = "reviewId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Review toReview(CreateReviewCommand command, Booking booking, User reviewer,
                    User reviewee, Vehicle vehicle, ReviewType reviewType);

    // ── Response mapping ────────────────────────────────────────────

    @Mapping(source = "booking.bookingId", target = "bookingId")
    @Mapping(source = "vehicle.vehicleId", target = "vehicleId")
    @Mapping(expression = "java(review.getVehicle().getYear() + \" \" + review.getVehicle().getMake() + \" \" + review.getVehicle().getModel())", target = "vehicleSummary")
    @Mapping(source = "reviewer.userId", target = "reviewerUserId")
    @Mapping(expression = "java(review.getReviewer().getFirstName() + \" \" + review.getReviewer().getLastName())", target = "reviewerName")
    @Mapping(source = "reviewee.userId", target = "revieweeUserId")
    @Mapping(expression = "java(review.getReviewee().getFirstName() + \" \" + review.getReviewee().getLastName())", target = "revieweeName")
    ReviewResponse toReviewResponse(Review review);
}
