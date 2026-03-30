package com.turbo.booking.repository;

import com.turbo.booking.model.BookingPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingPhotoRepository extends JpaRepository<BookingPhoto, Long> {

    /**
     * Returns all photos for the given booking filtered by photo type ("PICKUP" or "RETURN").
     */
    List<BookingPhoto> findByBookingBookingIdAndPhotoType(Long bookingId, String photoType);
}
