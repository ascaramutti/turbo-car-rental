package com.turbo.booking.controller;

import com.turbo.booking.controller.mapper.AdminBookingControllerMapper;
import com.turbo.booking.dto.AdminBookingDetailResponse;
import com.turbo.booking.dto.AdminBookingResponse;
import com.turbo.booking.fixture.BookingFixture;
import com.turbo.booking.model.Booking;
import com.turbo.booking.service.BookingService;
import com.turbo.exception.BusinessException;
import com.turbo.exception.GlobalExceptionHandler;
import com.turbo.exception.error.BookingErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminBookingController")
class AdminBookingControllerTest {

    @Mock private BookingService bookingService;
    @Mock private AdminBookingControllerMapper controllerMapper;

    @InjectMocks private AdminBookingController adminBookingController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminBookingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── GET /api/admin/bookings ───────────────────────────────────────

    @Nested
    @DisplayName("GET /api/admin/bookings")
    class GetAllBookings {

        @Test
        @DisplayName("No filters - returns 200 with all bookings")
        void getAllBookings_noFilters_returns200() throws Exception {
            Booking booking = BookingFixture.pendingBooking();
            AdminBookingResponse response = BookingFixture.adminBookingResponse();

            when(bookingService.getAllBookings(null, null, null))
                    .thenReturn(List.of(booking));
            when(controllerMapper.toAdminBookingResponseList(List.of(booking)))
                    .thenReturn(List.of(response));

            mockMvc.perform(get("/api/admin/bookings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].bookingId").value(BookingFixture.BOOKING_ID))
                    .andExpect(jsonPath("$[0].driverId").value(BookingFixture.DRIVER_ID))
                    .andExpect(jsonPath("$[0].ownerId").value(BookingFixture.OWNER_ID));
        }

        @Test
        @DisplayName("With status filter - returns 200 with filtered bookings")
        void getAllBookings_withStatusFilter_returns200() throws Exception {
            Booking booking = BookingFixture.confirmedBooking();
            AdminBookingResponse response = BookingFixture.adminBookingResponse();
            response.setStatus("CONFIRMED");

            when(bookingService.getAllBookings("CONFIRMED", null, null))
                    .thenReturn(List.of(booking));
            when(controllerMapper.toAdminBookingResponseList(List.of(booking)))
                    .thenReturn(List.of(response));

            mockMvc.perform(get("/api/admin/bookings").param("status", "CONFIRMED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
        }

        @Test
        @DisplayName("With driverId and vehicleId filters - returns 200")
        void getAllBookings_withDriverAndVehicleFilters_returns200() throws Exception {
            Booking booking = BookingFixture.pendingBooking();
            AdminBookingResponse response = BookingFixture.adminBookingResponse();

            when(bookingService.getAllBookings(null, BookingFixture.DRIVER_ID, BookingFixture.VEHICLE_ID))
                    .thenReturn(List.of(booking));
            when(controllerMapper.toAdminBookingResponseList(List.of(booking)))
                    .thenReturn(List.of(response));

            mockMvc.perform(get("/api/admin/bookings")
                            .param("driverId", String.valueOf(BookingFixture.DRIVER_ID))
                            .param("vehicleId", String.valueOf(BookingFixture.VEHICLE_ID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].bookingId").value(BookingFixture.BOOKING_ID));
        }

        @Test
        @DisplayName("Returns empty list - returns 200 with empty array")
        void getAllBookings_noResults_returns200WithEmptyList() throws Exception {
            when(bookingService.getAllBookings(null, null, null)).thenReturn(List.of());
            when(controllerMapper.toAdminBookingResponseList(List.of())).thenReturn(List.of());

            mockMvc.perform(get("/api/admin/bookings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    // ── GET /api/admin/bookings/{bookingId} ───────────────────────────

    @Nested
    @DisplayName("GET /api/admin/bookings/{bookingId}")
    class GetBookingDetail {

        @Test
        @DisplayName("Booking found - returns 200 with admin detail")
        void getBookingDetail_found_returns200() throws Exception {
            Booking booking = BookingFixture.pendingBooking();
            AdminBookingDetailResponse response = BookingFixture.adminBookingDetailResponse();

            when(bookingService.getBookingById(BookingFixture.BOOKING_ID)).thenReturn(booking);
            when(controllerMapper.toAdminBookingDetailResponse(booking)).thenReturn(response);

            mockMvc.perform(get("/api/admin/bookings/{bookingId}", BookingFixture.BOOKING_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookingId").value(BookingFixture.BOOKING_ID))
                    .andExpect(jsonPath("$.driverId").value(BookingFixture.DRIVER_ID))
                    .andExpect(jsonPath("$.ownerId").value(BookingFixture.OWNER_ID))
                    .andExpect(jsonPath("$.vehicleMake").value("Toyota"));
        }

        @Test
        @DisplayName("Booking not found - returns 404 with BOOK-011")
        void getBookingDetail_notFound_returns404() throws Exception {
            when(bookingService.getBookingById(999L))
                    .thenThrow(new BusinessException(BookingErrorCode.BOOKING_NOT_FOUND));

            mockMvc.perform(get("/api/admin/bookings/{bookingId}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("BOOK-011"));
        }
    }
}
