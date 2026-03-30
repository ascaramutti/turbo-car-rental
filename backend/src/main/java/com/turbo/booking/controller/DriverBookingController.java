package com.turbo.booking.controller;

import com.turbo.booking.controller.mapper.DriverBookingControllerMapper;
import com.turbo.booking.dto.BookingDetailResponse;
import com.turbo.booking.dto.BookingResponse;
import com.turbo.booking.dto.CancelBookingRequest;
import com.turbo.booking.dto.CreateBookingRequest;
import com.turbo.booking.dto.DriverHoursSummaryResponse;
import com.turbo.booking.dto.VehicleBookingDetailResponse;
import com.turbo.booking.dto.VehicleLocationResponse;
import com.turbo.booking.dto.VehicleSearchResponse;
import com.turbo.booking.model.Booking;
import com.turbo.booking.service.BookingService;
import com.turbo.booking.service.result.DriverHoursSummary;
import com.turbo.booking.service.result.LocationResult;
import com.turbo.booking.service.result.VehicleSearchResult;
import com.turbo.booking.validation.BookingValidationConstraints;
import com.turbo.config.SecurityHelper;
import com.turbo.user.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/driver/bookings")
@RequiredArgsConstructor
public class DriverBookingController {

    private final BookingService bookingService;
    private final DriverBookingControllerMapper controllerMapper;
    private final SecurityHelper securityHelper;

    @GetMapping("/hours-summary")
    public ResponseEntity<DriverHoursSummaryResponse> getHoursSummary() {
        User user = securityHelper.getCurrentUser();
        DriverHoursSummary summary = bookingService.getDriverHoursSummary(user.getUserId());
        return ResponseEntity.ok(controllerMapper.toDriverHoursSummaryResponse(summary));
    }

    @GetMapping("/vehicles/search")
    public ResponseEntity<List<VehicleSearchResponse>> searchAvailableVehicles(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false, defaultValue = "" + BookingValidationConstraints.DEFAULT_SEARCH_RADIUS_KM) Double radiusKm,
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String fuelType) {

        User user = securityHelper.getCurrentUser();
        List<VehicleSearchResult> results = bookingService.searchAvailableVehicles(
                controllerMapper.toSearchCommand(user.getUserId(), latitude, longitude, radiusKm,
                        startTime, endTime, category, serviceType, minPrice, maxPrice, fuelType));
        return ResponseEntity.ok(controllerMapper.toVehicleSearchResponseList(results));
    }

    @GetMapping("/vehicles/{vehicleId}")
    public ResponseEntity<VehicleBookingDetailResponse> getVehicleDetail(@PathVariable Long vehicleId) {
        User user = securityHelper.getCurrentUser();
        VehicleSearchResult result = bookingService.getVehicleDetail(vehicleId, user.getUserId());
        return ResponseEntity.ok(controllerMapper.toVehicleBookingDetailResponse(result));
    }

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        User user = securityHelper.getCurrentUser();
        Booking booking = bookingService.createBooking(
                controllerMapper.toCreateCommand(request, user.getUserId()));
        return ResponseEntity.ok(controllerMapper.toBookingResponse(booking));
    }

    @GetMapping
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            @RequestParam(required = false) String status) {

        User user = securityHelper.getCurrentUser();
        List<Booking> bookings = bookingService.getDriverBookings(user.getUserId(), status);
        return ResponseEntity.ok(controllerMapper.toBookingResponseList(bookings));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingDetailResponse> getBookingDetail(@PathVariable Long bookingId) {
        User user = securityHelper.getCurrentUser();
        Booking booking = bookingService.getBookingForUser(
                controllerMapper.toGetCommand(bookingId, user.getUserId()));
        return ResponseEntity.ok(controllerMapper.toBookingDetailResponse(booking));
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

    @PutMapping("/{bookingId}/start")
    public ResponseEntity<BookingResponse> startBooking(
            @PathVariable Long bookingId,
            @RequestParam("pickupPhotos") List<MultipartFile> pickupPhotos) {

        User user = securityHelper.getCurrentUser();
        Booking booking = bookingService.startBooking(
                controllerMapper.toStartCommand(bookingId, user.getUserId(), pickupPhotos));
        return ResponseEntity.ok(controllerMapper.toBookingResponse(booking));
    }

    @PutMapping("/{bookingId}/complete")
    public ResponseEntity<BookingResponse> completeBooking(
            @PathVariable Long bookingId,
            @RequestParam("returnPhotos") List<MultipartFile> returnPhotos) {

        User user = securityHelper.getCurrentUser();
        Booking booking = bookingService.completeBooking(
                controllerMapper.toCompleteCommand(bookingId, user.getUserId(), returnPhotos));
        return ResponseEntity.ok(controllerMapper.toBookingResponse(booking));
    }

    @GetMapping("/{bookingId}/vehicle-location")
    public ResponseEntity<VehicleLocationResponse> getVehicleLocation(@PathVariable Long bookingId) {
        User user = securityHelper.getCurrentUser();
        LocationResult result = bookingService.getVehicleLocation(
                controllerMapper.toGetCommand(bookingId, user.getUserId()));
        return ResponseEntity.ok(controllerMapper.toVehicleLocationResponse(result));
    }

}
