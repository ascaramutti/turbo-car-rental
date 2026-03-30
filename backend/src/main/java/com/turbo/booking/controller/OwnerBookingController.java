package com.turbo.booking.controller;

import com.turbo.booking.controller.mapper.OwnerBookingControllerMapper;
import com.turbo.booking.dto.BookingDetailResponse;
import com.turbo.booking.dto.BookingResponse;
import com.turbo.booking.dto.CancelBookingRequest;
import com.turbo.booking.dto.RejectBookingRequest;
import com.turbo.booking.model.Booking;
import com.turbo.booking.service.BookingService;
import com.turbo.config.SecurityHelper;
import com.turbo.user.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/owner/bookings")
@RequiredArgsConstructor
public class OwnerBookingController {

    private final BookingService bookingService;
    private final OwnerBookingControllerMapper controllerMapper;
    private final SecurityHelper securityHelper;

    @GetMapping
    public ResponseEntity<List<BookingResponse>> getOwnerBookings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long vehicleId) {

        User user = securityHelper.getCurrentUser();
        List<Booking> bookings = bookingService.getOwnerBookings(user.getUserId(), status, vehicleId);
        return ResponseEntity.ok(controllerMapper.toBookingResponseList(bookings));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingDetailResponse> getBookingDetail(@PathVariable Long bookingId) {
        User user = securityHelper.getCurrentUser();
        Booking booking = bookingService.getBookingForUser(
                controllerMapper.toGetCommand(bookingId, user.getUserId()));
        return ResponseEntity.ok(controllerMapper.toBookingDetailResponse(booking));
    }

    @PutMapping("/{bookingId}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(@PathVariable Long bookingId) {
        User user = securityHelper.getCurrentUser();
        Booking booking = bookingService.confirmBooking(
                controllerMapper.toConfirmCommand(bookingId, user.getUserId()));
        return ResponseEntity.ok(controllerMapper.toBookingResponse(booking));
    }

    @PutMapping("/{bookingId}/reject")
    public ResponseEntity<BookingResponse> rejectBooking(
            @PathVariable Long bookingId,
            @Valid @RequestBody RejectBookingRequest request) {

        User user = securityHelper.getCurrentUser();
        Booking booking = bookingService.rejectBooking(
                controllerMapper.toRejectCommand(bookingId, user.getUserId(), request.getReason()));
        return ResponseEntity.ok(controllerMapper.toBookingResponse(booking));
    }

    @PutMapping("/{bookingId}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable Long bookingId,
            @Valid @RequestBody CancelBookingRequest request) {

        User user = securityHelper.getCurrentUser();
        Booking booking = bookingService.cancelBooking(
                controllerMapper.toCancelCommand(bookingId, user.getUserId(), request.getReason()));
        return ResponseEntity.ok(controllerMapper.toBookingResponse(booking));
    }
}
