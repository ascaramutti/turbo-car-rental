package com.turbo.review.service.impl;

import com.turbo.booking.model.Booking;
import com.turbo.booking.model.enums.BookingStatus;
import com.turbo.booking.repository.BookingRepository;
import com.turbo.exception.BusinessException;
import com.turbo.exception.error.ReviewErrorCode;
import com.turbo.review.dto.ReviewResponse;
import com.turbo.review.model.Review;
import com.turbo.review.model.enums.ReviewType;
import com.turbo.review.repository.ReviewRepository;
import com.turbo.review.service.ReviewService;
import com.turbo.review.service.command.CreateReviewCommand;
import com.turbo.review.service.mapper.ReviewServiceMapper;
import com.turbo.user.model.User;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final ReviewServiceMapper serviceMapper;

    @Override
    @Transactional
    public ReviewResponse createReview(CreateReviewCommand command) {
        Booking booking = findBookingById(command.getBookingId());
        validateBookingCompleted(booking);

        User reviewer = resolveReviewer(booking, command.getUserId());
        User reviewee = resolveReviewee(booking, command.getUserId());
        ReviewType reviewType = resolveReviewType(booking, command.getUserId());

        validateNotAlreadyReviewed(command.getBookingId(), command.getUserId());

        Review review = serviceMapper.toReview(
                command, booking, reviewer, reviewee, booking.getVehicle(), reviewType);

        Review saved = reviewRepository.save(review);
        return serviceMapper.toReviewResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsForUser(Long userId) {
        return reviewRepository.findByRevieweeUserIdOrderByCreatedAtDesc(userId).stream()
                .map(serviceMapper::toReviewResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsForBooking(Long bookingId) {
        return reviewRepository.findByBookingBookingIdOrderByCreatedAtDesc(bookingId).stream()
                .map(serviceMapper::toReviewResponse)
                .toList();
    }

    // ── Lookup helpers ──────────────────────────────────────────────────

    private Booking findBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(ReviewErrorCode.BOOKING_NOT_FOUND));
    }

    // ── Validation helpers ──────────────────────────────────────────────

    private void validateBookingCompleted(Booking booking) {
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BusinessException(ReviewErrorCode.BOOKING_NOT_COMPLETED);
        }
    }

    private void validateNotAlreadyReviewed(Long bookingId, Long userId) {
        if (reviewRepository.existsByBookingBookingIdAndReviewerUserId(bookingId, userId)) {
            throw new BusinessException(ReviewErrorCode.REVIEW_ALREADY_SUBMITTED);
        }
    }

    // ── Resolution helpers ──────────────────────────────────────────────

    /** Resolves the reviewer User from the booking based on the authenticated userId. */
    private User resolveReviewer(Booking booking, Long userId) {
        if (booking.getDriver().getUserId().equals(userId)) {
            return booking.getDriver();
        }
        if (booking.getVehicle().getOwner().getUserId().equals(userId)) {
            return booking.getVehicle().getOwner();
        }
        throw new BusinessException(ReviewErrorCode.UNAUTHORIZED_REVIEW);
    }

    /** Resolves the reviewee (the other party) from the booking. */
    private User resolveReviewee(Booking booking, Long userId) {
        if (booking.getDriver().getUserId().equals(userId)) {
            return booking.getVehicle().getOwner();
        }
        return booking.getDriver();
    }

    /** Resolves the review type based on who is writing the review. */
    private ReviewType resolveReviewType(Booking booking, Long userId) {
        if (booking.getDriver().getUserId().equals(userId)) {
            return ReviewType.DRIVER_TO_OWNER;
        }
        return ReviewType.OWNER_TO_DRIVER;
    }
}
