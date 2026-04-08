package com.turbo.payment.repository;

import com.turbo.payment.model.Payment;
import com.turbo.payment.model.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingBookingId(Long bookingId);

    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    /** Sums ownerPayout for an owner's payments with the given status. */
    @Query("SELECT COALESCE(SUM(p.ownerPayout), 0) FROM Payment p " +
           "WHERE p.booking.vehicle.owner.userId = :ownerId AND p.status = :status")
    BigDecimal sumOwnerPayoutByStatus(@Param("ownerId") Long ownerId, @Param("status") PaymentStatus status);

    /** Sums ownerPayout for an owner's COMPLETED payments within a date range. */
    @Query("SELECT COALESCE(SUM(p.ownerPayout), 0) FROM Payment p " +
           "WHERE p.booking.vehicle.owner.userId = :ownerId AND p.status = 'COMPLETED' " +
           "AND p.createdAt >= :start AND p.createdAt < :end")
    BigDecimal sumOwnerPayoutInRange(@Param("ownerId") Long ownerId,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    /** Counts payments for an owner with the given status. */
    @Query("SELECT COUNT(p) FROM Payment p " +
           "WHERE p.booking.vehicle.owner.userId = :ownerId AND p.status = :status")
    long countByOwnerAndStatus(@Param("ownerId") Long ownerId, @Param("status") PaymentStatus status);

    /** Finds all payments for an owner's vehicles, ordered by most recent. */
    @Query("SELECT p FROM Payment p " +
           "WHERE p.booking.vehicle.owner.userId = :ownerId " +
           "ORDER BY p.createdAt DESC")
    List<Payment> findByOwnerOrderByCreatedAtDesc(@Param("ownerId") Long ownerId);
}
