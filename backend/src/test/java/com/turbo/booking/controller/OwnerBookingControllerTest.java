package com.turbo.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.turbo.booking.controller.mapper.OwnerBookingControllerMapper;
import com.turbo.booking.dto.BookingDetailResponse;
import com.turbo.booking.dto.BookingResponse;
import com.turbo.booking.dto.CancelBookingRequest;
import com.turbo.booking.dto.RejectBookingRequest;
import com.turbo.booking.fixture.BookingFixture;
import com.turbo.booking.model.Booking;
import com.turbo.booking.service.BookingService;
import com.turbo.booking.service.command.CancelBookingCommand;
import com.turbo.booking.service.command.ConfirmBookingCommand;
import com.turbo.booking.service.command.GetBookingCommand;
import com.turbo.booking.service.command.RejectBookingCommand;
import com.turbo.config.SecurityHelper;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("OwnerBookingController")
class OwnerBookingControllerTest {

    @Mock private BookingService bookingService;
    @Mock private OwnerBookingControllerMapper controllerMapper;
    @Mock private SecurityHelper securityHelper;

    @InjectMocks private OwnerBookingController ownerBookingController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ownerBookingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        lenient().when(securityHelper.getCurrentUser()).thenReturn(BookingFixture.testCarOwner());
    }

    // ── GET /api/owner/bookings ───────────────────────────────────────

    @Nested
    @DisplayName("GET /api/owner/bookings")
    class GetOwnerBookings {

        @Test
        @DisplayName("No filters - returns 200 with all bookings")
        void getOwnerBookings_noFilters_returns200() throws Exception {
            Booking booking = BookingFixture.pendingBooking();
            BookingResponse response = BookingFixture.pendingBookingResponse();

            when(bookingService.getOwnerBookings(BookingFixture.OWNER_ID, null, null))
                    .thenReturn(List.of(booking));
            when(controllerMapper.toBookingResponseList(List.of(booking)))
                    .thenReturn(List.of(response));

            mockMvc.perform(get("/api/owner/bookings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].bookingId").value(BookingFixture.BOOKING_ID));
        }

        @Test
        @DisplayName("With status and vehicleId filters - returns 200 with filtered bookings")
        void getOwnerBookings_withFilters_returns200() throws Exception {
            Booking booking = BookingFixture.confirmedBooking();
            BookingResponse response = BookingFixture.pendingBookingResponse();
            response.setStatus("CONFIRMED");

            when(bookingService.getOwnerBookings(BookingFixture.OWNER_ID, "CONFIRMED", BookingFixture.VEHICLE_ID))
                    .thenReturn(List.of(booking));
            when(controllerMapper.toBookingResponseList(List.of(booking)))
                    .thenReturn(List.of(response));

            mockMvc.perform(get("/api/owner/bookings")
                            .param("status", "CONFIRMED")
                            .param("vehicleId", String.valueOf(BookingFixture.VEHICLE_ID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
        }
    }

    // ── GET /api/owner/bookings/{bookingId} ───────────────────────────

    @Nested
    @DisplayName("GET /api/owner/bookings/{bookingId}")
    class GetBookingDetail {

        @Test
        @DisplayName("Booking found - returns 200 with detail")
        void getBookingDetail_found_returns200() throws Exception {
            GetBookingCommand command = BookingFixture.getBookingCommandForOwner();
            Booking booking = BookingFixture.pendingBooking();
            BookingDetailResponse response = BookingFixture.pendingBookingDetailResponse();

            when(controllerMapper.toGetCommand(BookingFixture.BOOKING_ID, BookingFixture.OWNER_ID))
                    .thenReturn(command);
            when(bookingService.getBookingForUser(command)).thenReturn(booking);
            when(controllerMapper.toBookingDetailResponse(booking)).thenReturn(response);

            mockMvc.perform(get("/api/owner/bookings/{bookingId}", BookingFixture.BOOKING_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookingId").value(BookingFixture.BOOKING_ID));
        }

        @Test
        @DisplayName("Booking not found - returns 404 with BOOK-011")
        void getBookingDetail_notFound_returns404() throws Exception {
            GetBookingCommand command = new GetBookingCommand();
            when(controllerMapper.toGetCommand(999L, BookingFixture.OWNER_ID)).thenReturn(command);
            when(bookingService.getBookingForUser(command))
                    .thenThrow(new BusinessException(BookingErrorCode.BOOKING_NOT_FOUND));

            mockMvc.perform(get("/api/owner/bookings/{bookingId}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("BOOK-011"));
        }

        @Test
        @DisplayName("Access denied (another owner) - returns 403 with BOOK-012")
        void getBookingDetail_accessDenied_returns403() throws Exception {
            GetBookingCommand command = new GetBookingCommand();
            when(controllerMapper.toGetCommand(BookingFixture.BOOKING_ID, BookingFixture.OWNER_ID))
                    .thenReturn(command);
            when(bookingService.getBookingForUser(command))
                    .thenThrow(new BusinessException(BookingErrorCode.BOOKING_ACCESS_DENIED));

            mockMvc.perform(get("/api/owner/bookings/{bookingId}", BookingFixture.BOOKING_ID))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("BOOK-012"));
        }
    }

    // ── PUT /api/owner/bookings/{bookingId}/confirm ───────────────────

    @Nested
    @DisplayName("PUT /api/owner/bookings/{bookingId}/confirm")
    class ConfirmBooking {

        @Test
        @DisplayName("Valid confirm - returns 200 with CONFIRMED status")
        void confirmBooking_valid_returns200() throws Exception {
            ConfirmBookingCommand command = BookingFixture.confirmCommand();
            Booking booking = BookingFixture.confirmedBooking();
            BookingResponse response = BookingFixture.pendingBookingResponse();
            response.setStatus("CONFIRMED");

            when(controllerMapper.toConfirmCommand(BookingFixture.BOOKING_ID, BookingFixture.OWNER_ID))
                    .thenReturn(command);
            when(bookingService.confirmBooking(command)).thenReturn(booking);
            when(controllerMapper.toBookingResponse(booking)).thenReturn(response);

            mockMvc.perform(put("/api/owner/bookings/{bookingId}/confirm", BookingFixture.BOOKING_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));
        }

        @Test
        @DisplayName("Booking not PENDING - returns 400 with BOOK-013")
        void confirmBooking_notPending_returns400() throws Exception {
            ConfirmBookingCommand command = new ConfirmBookingCommand();
            when(controllerMapper.toConfirmCommand(anyLong(), anyLong())).thenReturn(command);
            when(bookingService.confirmBooking(command))
                    .thenThrow(new BusinessException(BookingErrorCode.ONLY_PENDING_CAN_BE_CONFIRMED));

            mockMvc.perform(put("/api/owner/bookings/{bookingId}/confirm", BookingFixture.BOOKING_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BOOK-013"));
        }
    }

    // ── PUT /api/owner/bookings/{bookingId}/reject ────────────────────

    @Nested
    @DisplayName("PUT /api/owner/bookings/{bookingId}/reject")
    class RejectBooking {

        @Test
        @DisplayName("Valid reject - returns 200 with REJECTED status")
        void rejectBooking_valid_returns200() throws Exception {
            RejectBookingRequest request = new RejectBookingRequest();
            request.setReason("Vehicle unavailable");

            RejectBookingCommand command = BookingFixture.rejectCommand();
            Booking booking = BookingFixture.pendingBooking();
            booking.setStatus(com.turbo.booking.model.enums.BookingStatus.REJECTED);
            BookingResponse response = BookingFixture.pendingBookingResponse();
            response.setStatus("REJECTED");

            when(controllerMapper.toRejectCommand(BookingFixture.BOOKING_ID, BookingFixture.OWNER_ID, "Vehicle unavailable"))
                    .thenReturn(command);
            when(bookingService.rejectBooking(command)).thenReturn(booking);
            when(controllerMapper.toBookingResponse(booking)).thenReturn(response);

            mockMvc.perform(put("/api/owner/bookings/{bookingId}/reject", BookingFixture.BOOKING_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"));
        }

        @Test
        @DisplayName("Booking not PENDING - returns 400 with BOOK-014")
        void rejectBooking_notPending_returns400() throws Exception {
            RejectBookingRequest request = new RejectBookingRequest();
            request.setReason("Vehicle unavailable");

            RejectBookingCommand command = new RejectBookingCommand();
            when(controllerMapper.toRejectCommand(anyLong(), anyLong(), anyString()))
                    .thenReturn(command);
            when(bookingService.rejectBooking(command))
                    .thenThrow(new BusinessException(BookingErrorCode.ONLY_PENDING_CAN_BE_REJECTED));

            mockMvc.perform(put("/api/owner/bookings/{bookingId}/reject", BookingFixture.BOOKING_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BOOK-014"));
        }

        @Test
        @DisplayName("Missing reason - returns 400 with VALIDATION-001")
        void rejectBooking_missingReason_returns400() throws Exception {
            RejectBookingRequest request = new RejectBookingRequest();

            mockMvc.perform(put("/api/owner/bookings/{bookingId}/reject", BookingFixture.BOOKING_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        @Test
        @DisplayName("Reason too long (>500 chars) - returns 400 with VALIDATION-001")
        void rejectBooking_reasonTooLong_returns400() throws Exception {
            RejectBookingRequest request = new RejectBookingRequest();
            request.setReason("A".repeat(501));

            mockMvc.perform(put("/api/owner/bookings/{bookingId}/reject", BookingFixture.BOOKING_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        @Test
        @DisplayName("Reason with injection characters - returns 400 with VALIDATION-001")
        void rejectBooking_reasonWithInvalidChars_returns400() throws Exception {
            RejectBookingRequest request = new RejectBookingRequest();
            request.setReason("<script>xss</script>");

            mockMvc.perform(put("/api/owner/bookings/{bookingId}/reject", BookingFixture.BOOKING_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }
    }

    // ── PUT /api/owner/bookings/{bookingId}/cancel ────────────────────

    @Nested
    @DisplayName("PUT /api/owner/bookings/{bookingId}/cancel")
    class CancelBooking {

        @Test
        @DisplayName("Valid owner cancel - returns 200 with CANCELLED status")
        void cancelBooking_valid_returns200() throws Exception {
            CancelBookingRequest request = new CancelBookingRequest();
            request.setReason("Vehicle under maintenance");

            CancelBookingCommand command = BookingFixture.cancelCommandForOwner();
            Booking booking = BookingFixture.cancelledBooking();
            BookingResponse response = BookingFixture.pendingBookingResponse();
            response.setStatus("CANCELLED");

            when(controllerMapper.toCancelCommand(BookingFixture.BOOKING_ID, BookingFixture.OWNER_ID, "Vehicle under maintenance"))
                    .thenReturn(command);
            when(bookingService.cancelBooking(command)).thenReturn(booking);
            when(controllerMapper.toBookingResponse(booking)).thenReturn(response);

            mockMvc.perform(put("/api/owner/bookings/{bookingId}/cancel", BookingFixture.BOOKING_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("Invalid status for owner cancellation - returns 400 with BOOK-015")
        void cancelBooking_invalidStatus_returns400() throws Exception {
            CancelBookingRequest request = new CancelBookingRequest();
            request.setReason("Reason");

            CancelBookingCommand command = new CancelBookingCommand();
            when(controllerMapper.toCancelCommand(anyLong(), anyLong(), anyString()))
                    .thenReturn(command);
            when(bookingService.cancelBooking(command))
                    .thenThrow(new BusinessException(BookingErrorCode.INVALID_STATUS_FOR_CANCELLATION));

            mockMvc.perform(put("/api/owner/bookings/{bookingId}/cancel", BookingFixture.BOOKING_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BOOK-015"));
        }

        @Test
        @DisplayName("Missing reason - returns 400 with VALIDATION-001")
        void cancelBooking_missingReason_returns400() throws Exception {
            CancelBookingRequest request = new CancelBookingRequest();

            mockMvc.perform(put("/api/owner/bookings/{bookingId}/cancel", BookingFixture.BOOKING_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }
    }
}
