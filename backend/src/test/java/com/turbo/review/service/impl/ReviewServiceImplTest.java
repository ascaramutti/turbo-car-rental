package com.turbo.review.service.impl;

import com.turbo.booking.model.Booking;
import com.turbo.booking.repository.BookingRepository;
import com.turbo.exception.BusinessException;
import com.turbo.exception.error.ReviewErrorCode;
import com.turbo.review.dto.ReviewResponse;
import com.turbo.review.fixture.ReviewFixture;
import com.turbo.review.model.Review;
import com.turbo.review.model.enums.ReviewType;
import com.turbo.review.repository.ReviewRepository;
import com.turbo.review.service.command.CreateReviewCommand;
import com.turbo.review.service.mapper.ReviewServiceMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewServiceImpl")
class ReviewServiceImplTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private ReviewServiceMapper serviceMapper;

    @InjectMocks private ReviewServiceImpl reviewService;

    // ── Helpers ───────────────────────────────────────────────────────

    private void mockBookingFound(Booking booking) {
        when(bookingRepository.findById(booking.getBookingId())).thenReturn(Optional.of(booking));
    }

    private void mockBookingNotFound() {
        when(bookingRepository.findById(ReviewFixture.BOOKING_ID)).thenReturn(Optional.empty());
    }

    private void mockNoExistingReview() {
        when(reviewRepository.existsByBookingBookingIdAndReviewerUserId(any(), any()))
                .thenReturn(false);
    }

    private void mockMapperToReview() {
        when(serviceMapper.toReview(any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    Review r = new Review();
                    r.setRating(((CreateReviewCommand) inv.getArgument(0)).getRating());
                    r.setComment(((CreateReviewCommand) inv.getArgument(0)).getComment());
                    r.setBooking(inv.getArgument(1));
                    r.setReviewer(inv.getArgument(2));
                    r.setReviewee(inv.getArgument(3));
                    r.setVehicle(inv.getArgument(4));
                    r.setReviewType(inv.getArgument(5));
                    return r;
                });
    }

    private void mockReviewSaved() {
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            if (r.getReviewId() == null) r.setReviewId(ReviewFixture.REVIEW_ID);
            return r;
        });
    }

    private ReviewResponse dummyResponse() {
        return new ReviewResponse(ReviewFixture.REVIEW_ID, ReviewFixture.BOOKING_ID,
                ReviewFixture.VEHICLE_ID, "2022 Toyota Camry",
                ReviewFixture.DRIVER_USER_ID, "John Driver",
                ReviewFixture.OWNER_USER_ID, "Sarah Owner",
                ReviewType.DRIVER_TO_OWNER, 4, "Great vehicle", LocalDateTime.now());
    }

    // ── createReview ──────────────────────────────────────────────────

    @Nested
    @DisplayName("createReview()")
    class CreateReview {

        @Test
        @DisplayName("Happy path — driver reviews owner after completed booking")
        void createReview_driverReviewsOwner_success() {
            Booking booking = ReviewFixture.completedBooking();
            mockBookingFound(booking);
            mockNoExistingReview();
            mockMapperToReview();
            mockReviewSaved();
            when(serviceMapper.toReviewResponse(any(Review.class))).thenReturn(dummyResponse());

            ReviewResponse result = reviewService.createReview(ReviewFixture.createReviewCommandAsDriver());

            assertThat(result).isNotNull();
            assertThat(result.getReviewType()).isEqualTo(ReviewType.DRIVER_TO_OWNER);
            verify(reviewRepository).save(argThat(r -> {
                assertThat(r.getReviewType()).isEqualTo(ReviewType.DRIVER_TO_OWNER);
                assertThat(r.getRating()).isEqualTo(4);
                assertThat(r.getReviewer().getUserId()).isEqualTo(ReviewFixture.DRIVER_USER_ID);
                assertThat(r.getReviewee().getUserId()).isEqualTo(ReviewFixture.OWNER_USER_ID);
                return true;
            }));
        }

        @Test
        @DisplayName("Happy path — owner reviews driver after completed booking")
        void createReview_ownerReviewsDriver_success() {
            Booking booking = ReviewFixture.completedBooking();
            mockBookingFound(booking);
            mockNoExistingReview();
            mockMapperToReview();
            mockReviewSaved();

            ReviewResponse ownerResponse = new ReviewResponse(ReviewFixture.REVIEW_ID, ReviewFixture.BOOKING_ID,
                    ReviewFixture.VEHICLE_ID, "2022 Toyota Camry",
                    ReviewFixture.OWNER_USER_ID, "Sarah Owner",
                    ReviewFixture.DRIVER_USER_ID, "John Driver",
                    ReviewType.OWNER_TO_DRIVER, 5, "Excellent driver", LocalDateTime.now());
            when(serviceMapper.toReviewResponse(any(Review.class))).thenReturn(ownerResponse);

            ReviewResponse result = reviewService.createReview(ReviewFixture.createReviewCommandAsOwner());

            assertThat(result).isNotNull();
            verify(reviewRepository).save(argThat(r -> {
                assertThat(r.getReviewType()).isEqualTo(ReviewType.OWNER_TO_DRIVER);
                assertThat(r.getReviewer().getUserId()).isEqualTo(ReviewFixture.OWNER_USER_ID);
                assertThat(r.getReviewee().getUserId()).isEqualTo(ReviewFixture.DRIVER_USER_ID);
                return true;
            }));
        }

        @Test
        @DisplayName("REV-001: booking not found — throws BOOKING_NOT_FOUND")
        void createReview_bookingNotFound_throwsRev001() {
            mockBookingNotFound();

            assertThatThrownBy(() -> reviewService.createReview(ReviewFixture.createReviewCommandAsDriver()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ReviewErrorCode.BOOKING_NOT_FOUND);
        }

        @Test
        @DisplayName("REV-002: booking not completed — throws BOOKING_NOT_COMPLETED")
        void createReview_bookingNotCompleted_throwsRev002() {
            Booking booking = ReviewFixture.confirmedBooking();
            mockBookingFound(booking);

            assertThatThrownBy(() -> reviewService.createReview(ReviewFixture.createReviewCommandAsDriver()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ReviewErrorCode.BOOKING_NOT_COMPLETED);
        }

        @Test
        @DisplayName("REV-003: stranger tries to review — throws UNAUTHORIZED_REVIEW")
        void createReview_stranger_throwsRev003() {
            Booking booking = ReviewFixture.completedBooking();
            mockBookingFound(booking);

            CreateReviewCommand cmd = ReviewFixture.createReviewCommandAsDriver();
            cmd.setUserId(ReviewFixture.OTHER_USER_ID);

            assertThatThrownBy(() -> reviewService.createReview(cmd))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ReviewErrorCode.UNAUTHORIZED_REVIEW);
        }

        @Test
        @DisplayName("REV-004: duplicate review — throws REVIEW_ALREADY_SUBMITTED")
        void createReview_duplicate_throwsRev004() {
            Booking booking = ReviewFixture.completedBooking();
            mockBookingFound(booking);
            when(reviewRepository.existsByBookingBookingIdAndReviewerUserId(
                    ReviewFixture.BOOKING_ID, ReviewFixture.DRIVER_USER_ID)).thenReturn(true);

            assertThatThrownBy(() -> reviewService.createReview(ReviewFixture.createReviewCommandAsDriver()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ReviewErrorCode.REVIEW_ALREADY_SUBMITTED);
        }

        @Test
        @DisplayName("Comment is optional — null comment accepted")
        void createReview_nullComment_accepted() {
            Booking booking = ReviewFixture.completedBooking();
            mockBookingFound(booking);
            mockNoExistingReview();
            mockMapperToReview();
            mockReviewSaved();
            when(serviceMapper.toReviewResponse(any(Review.class))).thenReturn(dummyResponse());

            CreateReviewCommand cmd = ReviewFixture.createReviewCommandAsDriver();
            cmd.setComment(null);

            ReviewResponse result = reviewService.createReview(cmd);

            assertThat(result).isNotNull();
            verify(reviewRepository).save(argThat(r -> {
                assertThat(r.getComment()).isNull();
                return true;
            }));
        }
    }

    // ── getReviewsForUser ─────────────────────────────────────────────

    @Nested
    @DisplayName("getReviewsForUser()")
    class GetReviewsForUser {

        @Test
        @DisplayName("Returns reviews for user ordered by most recent")
        void getReviewsForUser_returnsReviews() {
            Review review = ReviewFixture.driverToOwnerReview();
            when(reviewRepository.findByRevieweeUserIdOrderByCreatedAtDesc(ReviewFixture.OWNER_USER_ID))
                    .thenReturn(List.of(review));
            when(serviceMapper.toReviewResponse(review)).thenReturn(dummyResponse());

            List<ReviewResponse> results = reviewService.getReviewsForUser(ReviewFixture.OWNER_USER_ID);

            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("Returns empty list when no reviews exist")
        void getReviewsForUser_noReviews_returnsEmpty() {
            when(reviewRepository.findByRevieweeUserIdOrderByCreatedAtDesc(ReviewFixture.OWNER_USER_ID))
                    .thenReturn(List.of());

            List<ReviewResponse> results = reviewService.getReviewsForUser(ReviewFixture.OWNER_USER_ID);

            assertThat(results).isEmpty();
        }
    }

    // ── getReviewsForBooking ──────────────────────────────────────────

    @Nested
    @DisplayName("getReviewsForBooking()")
    class GetReviewsForBooking {

        @Test
        @DisplayName("Returns reviews for booking")
        void getReviewsForBooking_returnsReviews() {
            Review review = ReviewFixture.driverToOwnerReview();
            when(reviewRepository.findByBookingBookingIdOrderByCreatedAtDesc(ReviewFixture.BOOKING_ID))
                    .thenReturn(List.of(review));
            when(serviceMapper.toReviewResponse(review)).thenReturn(dummyResponse());

            List<ReviewResponse> results = reviewService.getReviewsForBooking(ReviewFixture.BOOKING_ID);

            assertThat(results).hasSize(1);
        }
    }
}
