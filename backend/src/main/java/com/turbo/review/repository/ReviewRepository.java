package com.turbo.review.repository;

import com.turbo.review.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByBookingBookingIdAndReviewerUserId(Long bookingId, Long reviewerUserId);

    List<Review> findByRevieweeUserIdOrderByCreatedAtDesc(Long revieweeUserId);

    List<Review> findByBookingBookingIdOrderByCreatedAtDesc(Long bookingId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewee.userId = :revieweeId")
    Float averageRatingByRevieweeId(@Param("revieweeId") Long revieweeId);
}
