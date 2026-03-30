package com.turbo.booking.controller;

import com.turbo.booking.controller.mapper.AdminBookingControllerMapper;
import com.turbo.booking.dto.AdminBookingDetailResponse;
import com.turbo.booking.dto.AdminBookingResponse;
import com.turbo.booking.model.Booking;
import com.turbo.booking.service.BookingService;
import com.turbo.booking.validation.BookingValidationConstraints;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
@Validated
public class AdminBookingController {

    private final BookingService bookingService;
    private final AdminBookingControllerMapper controllerMapper;

    @GetMapping
    public ResponseEntity<List<AdminBookingResponse>> getAllBookings(
            @RequestParam(required = false) @Pattern(regexp = BookingValidationConstraints.BOOKING_STATUS_PATTERN) String status,
            @RequestParam(required = false) @Min(1) Long driverId,
            @RequestParam(required = false) @Min(1) Long vehicleId) {

        List<Booking> bookings = bookingService.getAllBookings(status, driverId, vehicleId);
        return ResponseEntity.ok(controllerMapper.toAdminBookingResponseList(bookings));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<AdminBookingDetailResponse> getBookingDetail(@PathVariable Long bookingId) {
        Booking booking = bookingService.getBookingById(bookingId);
        return ResponseEntity.ok(controllerMapper.toAdminBookingDetailResponse(booking));
    }
}
