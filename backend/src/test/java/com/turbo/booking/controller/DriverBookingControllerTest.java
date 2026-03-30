package com.turbo.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.turbo.booking.controller.mapper.DriverBookingControllerMapper;
import com.turbo.booking.dto.BookingDetailResponse;
import com.turbo.booking.dto.BookingResponse;
import com.turbo.booking.dto.CancelBookingRequest;
import com.turbo.booking.dto.CreateBookingRequest;
import com.turbo.booking.dto.DriverHoursSummaryResponse;
import com.turbo.booking.dto.VehicleBookingDetailResponse;
import com.turbo.booking.dto.VehicleLocationResponse;
import com.turbo.booking.dto.VehicleSearchResponse;
import com.turbo.booking.fixture.BookingFixture;
import com.turbo.booking.model.Booking;
import com.turbo.booking.service.BookingService;
import com.turbo.booking.service.command.CancelBookingCommand;
import com.turbo.booking.service.command.CompleteBookingCommand;
import com.turbo.booking.service.command.CreateBookingCommand;
import com.turbo.booking.service.command.GetBookingCommand;
import com.turbo.booking.service.command.SearchVehiclesCommand;
import com.turbo.booking.service.command.StartBookingCommand;
import com.turbo.booking.service.result.DriverHoursSummary;
import com.turbo.booking.service.result.LocationResult;
import com.turbo.booking.service.result.VehicleSearchResult;
import com.turbo.config.SecurityHelper;
import com.turbo.exception.BusinessException;
import com.turbo.exception.GlobalExceptionHandler;
import com.turbo.exception.error.BookingErrorCode;
import com.turbo.exception.error.VehicleErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("DriverBookingController")
class DriverBookingControllerTest {

    @Mock private BookingService bookingService;
    @Mock private DriverBookingControllerMapper controllerMapper;
    @Mock private SecurityHelper securityHelper;

    @InjectMocks private DriverBookingController driverBookingController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(driverBookingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        lenient().when(securityHelper.getCurrentUser()).thenReturn(BookingFixture.verifiedDriver());
    }

    // ── GET /api/driver/bookings/hours-summary ────────────────────────

    @Nested
    @DisplayName("GET /api/driver/bookings/hours-summary")
    class GetHoursSummary {

        @Test
        @DisplayName("Valid request - returns 200 with hours summary")
        void getHoursSummary_valid_returns200() throws Exception {
            DriverHoursSummary summary = BookingFixture.driverHoursSummary();
            DriverHoursSummaryResponse response = BookingFixture.driverHoursSummaryResponse();

            when(bookingService.getDriverHoursSummary(BookingFixture.DRIVER_ID)).thenReturn(summary);
            when(controllerMapper.toDriverHoursSummaryResponse(summary)).thenReturn(response);

            mockMvc.perform(get("/api/driver/bookings/hours-summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hoursUsedThisWeek").value(8))
                    .andExpect(jsonPath("$.maxHoursPerWeek").value(24))
                    .andExpect(jsonPath("$.hoursRemaining").value(16));
        }

        @Test
        @DisplayName("Driver at max hours - returns hoursRemaining of 0")
        void getHoursSummary_atMaxHours_returns200WithZeroRemaining() throws Exception {
            DriverHoursSummary summary = new DriverHoursSummary(24, 24, 0);
            DriverHoursSummaryResponse response = new DriverHoursSummaryResponse(24, 24, 0);

            when(bookingService.getDriverHoursSummary(BookingFixture.DRIVER_ID)).thenReturn(summary);
            when(controllerMapper.toDriverHoursSummaryResponse(summary)).thenReturn(response);

            mockMvc.perform(get("/api/driver/bookings/hours-summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hoursUsedThisWeek").value(24))
                    .andExpect(jsonPath("$.hoursRemaining").value(0));
        }
    }

    // ── GET /api/driver/bookings/vehicles/search ──────────────────────

    @Nested
    @DisplayName("GET /api/driver/bookings/vehicles/search")
    class SearchVehicles {

        @Test
        @DisplayName("Valid search with no filters - returns 200 with results")
        void searchVehicles_noFilters_returns200WithResults() throws Exception {
            SearchVehiclesCommand command = BookingFixture.searchVehiclesCommand();
            VehicleSearchResult result = BookingFixture.vehicleSearchResult();
            VehicleSearchResponse response = BookingFixture.vehicleSearchResponse();

            when(controllerMapper.toSearchCommand(anyLong(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any())).thenReturn(command);
            when(bookingService.searchAvailableVehicles(command)).thenReturn(List.of(result));
            when(controllerMapper.toVehicleSearchResponseList(List.of(result))).thenReturn(List.of(response));

            mockMvc.perform(get("/api/driver/bookings/vehicles/search"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].vehicleId").value(BookingFixture.VEHICLE_ID));
        }

        @Test
        @DisplayName("Search returns empty list - returns 200 with empty array")
        void searchVehicles_noResults_returns200WithEmptyList() throws Exception {
            SearchVehiclesCommand command = BookingFixture.searchVehiclesCommand();

            when(controllerMapper.toSearchCommand(anyLong(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any())).thenReturn(command);
            when(bookingService.searchAvailableVehicles(command)).thenReturn(List.of());
            when(controllerMapper.toVehicleSearchResponseList(List.of())).thenReturn(List.of());

            mockMvc.perform(get("/api/driver/bookings/vehicles/search"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    // ── GET /api/driver/bookings/vehicles/{vehicleId} ─────────────────

    @Nested
    @DisplayName("GET /api/driver/bookings/vehicles/{vehicleId}")
    class GetVehicleDetail {

        @Test
        @DisplayName("Vehicle found - returns 200 with vehicle detail")
        void getVehicleDetail_found_returns200() throws Exception {
            VehicleSearchResult result = BookingFixture.vehicleSearchResult();
            VehicleBookingDetailResponse response = new VehicleBookingDetailResponse();
            response.setVehicleId(BookingFixture.VEHICLE_ID);
            response.setMake("Toyota");

            when(bookingService.getVehicleDetail(BookingFixture.VEHICLE_ID, BookingFixture.DRIVER_ID))
                    .thenReturn(result);
            when(controllerMapper.toVehicleBookingDetailResponse(result)).thenReturn(response);

            mockMvc.perform(get("/api/driver/bookings/vehicles/{vehicleId}", BookingFixture.VEHICLE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.vehicleId").value(BookingFixture.VEHICLE_ID));
        }

        @Test
        @DisplayName("Vehicle not found - returns 404 with VEH-004")
        void getVehicleDetail_notFound_returns404() throws Exception {
            when(bookingService.getVehicleDetail(999L, BookingFixture.DRIVER_ID))
                    .thenThrow(new BusinessException(VehicleErrorCode.VEHICLE_NOT_FOUND));

            mockMvc.perform(get("/api/driver/bookings/vehicles/{vehicleId}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("VEH-004"));
        }
    }

    // ── POST /api/driver/bookings ─────────────────────────────────────

    @Nested
    @DisplayName("POST /api/driver/bookings")
    class CreateBooking {

        @Test
        @DisplayName("Valid request - returns 200 with BookingResponse")
        void createBooking_validRequest_returns200() throws Exception {
            CreateBookingRequest request = new CreateBookingRequest();
            request.setVehicleId(BookingFixture.VEHICLE_ID);
            request.setStartTime(BookingFixture.FUTURE_START);
            request.setEndTime(BookingFixture.FUTURE_END);

            CreateBookingCommand command = BookingFixture.createBookingCommand();
            Booking booking = BookingFixture.pendingBooking();
            BookingResponse response = BookingFixture.pendingBookingResponse();

            when(controllerMapper.toCreateCommand(any(CreateBookingRequest.class), anyLong()))
                    .thenReturn(command);
            when(bookingService.createBooking(command)).thenReturn(booking);
            when(controllerMapper.toBookingResponse(booking)).thenReturn(response);

            mockMvc.perform(post("/api/driver/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookingId").value(BookingFixture.BOOKING_ID))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("vehicleId is null - returns 400 with VALIDATION-001")
        void createBooking_nullVehicleId_returns400() throws Exception {
            CreateBookingRequest request = new CreateBookingRequest();
            request.setVehicleId(null);
            request.setStartTime(BookingFixture.FUTURE_START);
            request.setEndTime(BookingFixture.FUTURE_END);

            mockMvc.perform(post("/api/driver/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        @Test
        @DisplayName("startTime is null - returns 400 with VALIDATION-001")
        void createBooking_nullStartTime_returns400() throws Exception {
            CreateBookingRequest request = new CreateBookingRequest();
            request.setVehicleId(BookingFixture.VEHICLE_ID);
            request.setStartTime(null);
            request.setEndTime(BookingFixture.FUTURE_END);

            mockMvc.perform(post("/api/driver/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        @Test
        @DisplayName("startTime is in the past - returns 400 with VALIDATION-001")
        void createBooking_pastStartTime_returns400() throws Exception {
            CreateBookingRequest request = new CreateBookingRequest();
            request.setVehicleId(BookingFixture.VEHICLE_ID);
            request.setStartTime(LocalDateTime.now().minusHours(1));
            request.setEndTime(BookingFixture.FUTURE_END);

            mockMvc.perform(post("/api/driver/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        @Test
        @DisplayName("endTime is null - returns 400 with VALIDATION-001")
        void createBooking_nullEndTime_returns400() throws Exception {
            CreateBookingRequest request = new CreateBookingRequest();
            request.setVehicleId(BookingFixture.VEHICLE_ID);
            request.setStartTime(BookingFixture.FUTURE_START);
            request.setEndTime(null);

            mockMvc.perform(post("/api/driver/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }
    }

    // ── GET /api/driver/bookings ──────────────────────────────────────

    @Nested
    @DisplayName("GET /api/driver/bookings")
    class GetMyBookings {

        @Test
        @DisplayName("No status filter - returns 200 with all bookings")
        void getMyBookings_noFilter_returns200() throws Exception {
            Booking booking = BookingFixture.pendingBooking();
            BookingResponse response = BookingFixture.pendingBookingResponse();

            when(bookingService.getDriverBookings(BookingFixture.DRIVER_ID, null))
                    .thenReturn(List.of(booking));
            when(controllerMapper.toBookingResponseList(List.of(booking)))
                    .thenReturn(List.of(response));

            mockMvc.perform(get("/api/driver/bookings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].bookingId").value(BookingFixture.BOOKING_ID));
        }

        @Test
        @DisplayName("With status filter - returns 200 with filtered bookings")
        void getMyBookings_withStatusFilter_returns200() throws Exception {
            Booking booking = BookingFixture.pendingBooking();
            BookingResponse response = BookingFixture.pendingBookingResponse();

            when(bookingService.getDriverBookings(BookingFixture.DRIVER_ID, "PENDING"))
                    .thenReturn(List.of(booking));
            when(controllerMapper.toBookingResponseList(List.of(booking)))
                    .thenReturn(List.of(response));

            mockMvc.perform(get("/api/driver/bookings").param("status", "PENDING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("PENDING"));
        }
    }

    // ── GET /api/driver/bookings/{bookingId} ──────────────────────────

    @Nested
    @DisplayName("GET /api/driver/bookings/{bookingId}")
    class GetBookingDetail {

        @Test
        @DisplayName("Booking found - returns 200 with detail")
        void getBookingDetail_found_returns200() throws Exception {
            GetBookingCommand command = BookingFixture.getBookingCommandForDriver();
            Booking booking = BookingFixture.pendingBooking();
            BookingDetailResponse response = BookingFixture.pendingBookingDetailResponse();

            when(controllerMapper.toGetCommand(BookingFixture.BOOKING_ID, BookingFixture.DRIVER_ID))
                    .thenReturn(command);
            when(bookingService.getBookingForUser(command)).thenReturn(booking);
            when(controllerMapper.toBookingDetailResponse(booking)).thenReturn(response);

            mockMvc.perform(get("/api/driver/bookings/{bookingId}", BookingFixture.BOOKING_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookingId").value(BookingFixture.BOOKING_ID));
        }

        @Test
        @DisplayName("Booking not found - returns 404 with BOOK-011")
        void getBookingDetail_notFound_returns404() throws Exception {
            GetBookingCommand command = new GetBookingCommand();
            when(controllerMapper.toGetCommand(999L, BookingFixture.DRIVER_ID)).thenReturn(command);
            when(bookingService.getBookingForUser(command))
                    .thenThrow(new BusinessException(BookingErrorCode.BOOKING_NOT_FOUND));

            mockMvc.perform(get("/api/driver/bookings/{bookingId}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("BOOK-011"));
        }

        @Test
        @DisplayName("Access denied (another driver) - returns 403 with BOOK-012")
        void getBookingDetail_accessDenied_returns403() throws Exception {
            GetBookingCommand command = new GetBookingCommand();
            when(controllerMapper.toGetCommand(BookingFixture.BOOKING_ID, BookingFixture.DRIVER_ID))
                    .thenReturn(command);
            when(bookingService.getBookingForUser(command))
                    .thenThrow(new BusinessException(BookingErrorCode.BOOKING_ACCESS_DENIED));

            mockMvc.perform(get("/api/driver/bookings/{bookingId}", BookingFixture.BOOKING_ID))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("BOOK-012"));
        }
    }

    // ── PUT /api/driver/bookings/{bookingId}/cancel ───────────────────

    @Nested
    @DisplayName("PUT /api/driver/bookings/{bookingId}/cancel")
    class CancelBooking {

        @Test
        @DisplayName("Valid cancel - returns 200 with CANCELLED booking")
        void cancelBooking_valid_returns200() throws Exception {
            CancelBookingRequest request = new CancelBookingRequest();
            request.setReason("Plans changed");

            CancelBookingCommand command = BookingFixture.cancelCommandForDriver();
            Booking booking = BookingFixture.cancelledBooking();
            BookingResponse response = BookingFixture.pendingBookingResponse();
            response.setStatus("CANCELLED");

            when(controllerMapper.toCancelCommand(BookingFixture.BOOKING_ID, BookingFixture.DRIVER_ID, "Plans changed"))
                    .thenReturn(command);
            when(bookingService.cancelBooking(command)).thenReturn(booking);
            when(controllerMapper.toBookingResponse(booking)).thenReturn(response);

            mockMvc.perform(put("/api/driver/bookings/{bookingId}/cancel", BookingFixture.BOOKING_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("Invalid status for cancellation - returns 400 with BOOK-015")
        void cancelBooking_invalidStatus_returns400() throws Exception {
            CancelBookingRequest request = new CancelBookingRequest();
            request.setReason("Reason");

            CancelBookingCommand command = BookingFixture.cancelCommandForDriver();
            when(controllerMapper.toCancelCommand(anyLong(), anyLong(), anyString()))
                    .thenReturn(command);
            when(bookingService.cancelBooking(command))
                    .thenThrow(new BusinessException(BookingErrorCode.INVALID_STATUS_FOR_CANCELLATION));

            mockMvc.perform(put("/api/driver/bookings/{bookingId}/cancel", BookingFixture.BOOKING_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BOOK-015"));
        }

        @Test
        @DisplayName("Missing reason - returns 400 with VALIDATION-001")
        void cancelBooking_missingReason_returns400() throws Exception {
            CancelBookingRequest request = new CancelBookingRequest();
            // reason intentionally omitted

            mockMvc.perform(put("/api/driver/bookings/{bookingId}/cancel", BookingFixture.BOOKING_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        @Test
        @DisplayName("Reason too long (>500) - returns 400 with VALIDATION-001")
        void cancelBooking_reasonTooLong_returns400() throws Exception {
            CancelBookingRequest request = new CancelBookingRequest();
            request.setReason("A".repeat(501));

            mockMvc.perform(put("/api/driver/bookings/{bookingId}/cancel", BookingFixture.BOOKING_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        @Test
        @DisplayName("Reason with injection characters - returns 400 with VALIDATION-001")
        void cancelBooking_reasonWithInvalidChars_returns400() throws Exception {
            CancelBookingRequest request = new CancelBookingRequest();
            request.setReason("<script>alert('xss')</script>");

            mockMvc.perform(put("/api/driver/bookings/{bookingId}/cancel", BookingFixture.BOOKING_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }
    }

    // ── PUT /api/driver/bookings/{bookingId}/start ────────────────────

    @Nested
    @DisplayName("PUT /api/driver/bookings/{bookingId}/start")
    class StartBooking {

        @Test
        @DisplayName("Valid start with photos - returns 200")
        void startBooking_valid_returns200() throws Exception {
            MockMultipartFile photo = BookingFixture.validPhoto("pickupPhotos", "pickup.jpg");
            StartBookingCommand command = BookingFixture.startCommand();
            Booking booking = BookingFixture.inProgressBooking();
            BookingResponse response = BookingFixture.pendingBookingResponse();
            response.setStatus("IN_PROGRESS");

            when(controllerMapper.toStartCommand(eq(BookingFixture.BOOKING_ID), eq(BookingFixture.DRIVER_ID), any()))
                    .thenReturn(command);
            when(bookingService.startBooking(command)).thenReturn(booking);
            when(controllerMapper.toBookingResponse(booking)).thenReturn(response);

            mockMvc.perform(multipart("/api/driver/bookings/{bookingId}/start", BookingFixture.BOOKING_ID)
                            .file(photo)
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }

        @Test
        @DisplayName("Wrong status (not CONFIRMED) - returns 400 with BOOK-016")
        void startBooking_wrongStatus_returns400() throws Exception {
            MockMultipartFile photo = BookingFixture.validPhoto("pickupPhotos", "pickup.jpg");
            StartBookingCommand command = new StartBookingCommand();

            when(controllerMapper.toStartCommand(anyLong(), anyLong(), any()))
                    .thenReturn(command);
            when(bookingService.startBooking(command))
                    .thenThrow(new BusinessException(BookingErrorCode.ONLY_CONFIRMED_CAN_BE_STARTED));

            mockMvc.perform(multipart("/api/driver/bookings/{bookingId}/start", BookingFixture.BOOKING_ID)
                            .file(photo)
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BOOK-016"));
        }

        @Test
        @DisplayName("Too early to start - returns 400 with BOOK-017")
        void startBooking_tooEarly_returns400() throws Exception {
            MockMultipartFile photo = BookingFixture.validPhoto("pickupPhotos", "pickup.jpg");
            StartBookingCommand command = new StartBookingCommand();

            when(controllerMapper.toStartCommand(anyLong(), anyLong(), any()))
                    .thenReturn(command);
            when(bookingService.startBooking(command))
                    .thenThrow(new BusinessException(BookingErrorCode.START_TOO_EARLY));

            mockMvc.perform(multipart("/api/driver/bookings/{bookingId}/start", BookingFixture.BOOKING_ID)
                            .file(photo)
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BOOK-017"));
        }

        @Test
        @DisplayName("Start window expired - returns 400 with BOOK-018")
        void startBooking_windowExpired_returns400() throws Exception {
            MockMultipartFile photo = BookingFixture.validPhoto("pickupPhotos", "pickup.jpg");
            StartBookingCommand command = new StartBookingCommand();

            when(controllerMapper.toStartCommand(anyLong(), anyLong(), any()))
                    .thenReturn(command);
            when(bookingService.startBooking(command))
                    .thenThrow(new BusinessException(BookingErrorCode.START_WINDOW_EXPIRED));

            mockMvc.perform(multipart("/api/driver/bookings/{bookingId}/start", BookingFixture.BOOKING_ID)
                            .file(photo)
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BOOK-018"));
        }

        @Test
        @DisplayName("Missing pickup photos - returns 400 with BOOK-020")
        void startBooking_missingPhoto_returns400() throws Exception {
            StartBookingCommand command = new StartBookingCommand();

            when(controllerMapper.toStartCommand(anyLong(), anyLong(), any()))
                    .thenReturn(command);
            when(bookingService.startBooking(command))
                    .thenThrow(new BusinessException(BookingErrorCode.PICKUP_PHOTO_REQUIRED));

            MockMultipartFile emptyPhoto = new MockMultipartFile("pickupPhotos", "", "image/jpeg", new byte[0]);
            mockMvc.perform(multipart("/api/driver/bookings/{bookingId}/start", BookingFixture.BOOKING_ID)
                            .file(emptyPhoto)
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BOOK-020"));
        }

        @Test
        @DisplayName("Too many photos - returns 400 with BOOK-026")
        void startBooking_tooManyPhotos_returns400() throws Exception {
            MockMultipartFile photo = BookingFixture.validPhoto("pickupPhotos", "pickup.jpg");
            StartBookingCommand command = new StartBookingCommand();

            when(controllerMapper.toStartCommand(anyLong(), anyLong(), any()))
                    .thenReturn(command);
            when(bookingService.startBooking(command))
                    .thenThrow(new BusinessException(BookingErrorCode.TOO_MANY_PHOTOS));

            mockMvc.perform(multipart("/api/driver/bookings/{bookingId}/start", BookingFixture.BOOKING_ID)
                            .file(photo)
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BOOK-026"));
        }
    }

    // ── PUT /api/driver/bookings/{bookingId}/complete ─────────────────

    @Nested
    @DisplayName("PUT /api/driver/bookings/{bookingId}/complete")
    class CompleteBooking {

        @Test
        @DisplayName("Valid complete with photos - returns 200")
        void completeBooking_valid_returns200() throws Exception {
            MockMultipartFile photo = BookingFixture.validPhoto("returnPhotos", "return.jpg");
            CompleteBookingCommand command = BookingFixture.completeCommand();
            Booking booking = BookingFixture.completedBooking();
            BookingResponse response = BookingFixture.pendingBookingResponse();
            response.setStatus("COMPLETED");

            when(controllerMapper.toCompleteCommand(eq(BookingFixture.BOOKING_ID), eq(BookingFixture.DRIVER_ID), any()))
                    .thenReturn(command);
            when(bookingService.completeBooking(command)).thenReturn(booking);
            when(controllerMapper.toBookingResponse(booking)).thenReturn(response);

            mockMvc.perform(multipart("/api/driver/bookings/{bookingId}/complete", BookingFixture.BOOKING_ID)
                            .file(photo)
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("Wrong status (not IN_PROGRESS) - returns 400 with BOOK-019")
        void completeBooking_wrongStatus_returns400() throws Exception {
            MockMultipartFile photo = BookingFixture.validPhoto("returnPhotos", "return.jpg");
            CompleteBookingCommand command = new CompleteBookingCommand();

            when(controllerMapper.toCompleteCommand(anyLong(), anyLong(), any()))
                    .thenReturn(command);
            when(bookingService.completeBooking(command))
                    .thenThrow(new BusinessException(BookingErrorCode.ONLY_IN_PROGRESS_CAN_BE_COMPLETED));

            mockMvc.perform(multipart("/api/driver/bookings/{bookingId}/complete", BookingFixture.BOOKING_ID)
                            .file(photo)
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BOOK-019"));
        }

        @Test
        @DisplayName("Missing return photos - returns 400 with BOOK-021")
        void completeBooking_missingPhoto_returns400() throws Exception {
            CompleteBookingCommand command = new CompleteBookingCommand();

            when(controllerMapper.toCompleteCommand(anyLong(), anyLong(), any()))
                    .thenReturn(command);
            when(bookingService.completeBooking(command))
                    .thenThrow(new BusinessException(BookingErrorCode.RETURN_PHOTO_REQUIRED));

            MockMultipartFile emptyPhoto = new MockMultipartFile("returnPhotos", "", "image/jpeg", new byte[0]);
            mockMvc.perform(multipart("/api/driver/bookings/{bookingId}/complete", BookingFixture.BOOKING_ID)
                            .file(emptyPhoto)
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BOOK-021"));
        }

        @Test
        @DisplayName("Too many return photos - returns 400 with BOOK-026")
        void completeBooking_tooManyPhotos_returns400() throws Exception {
            MockMultipartFile photo = BookingFixture.validPhoto("returnPhotos", "return.jpg");
            CompleteBookingCommand command = new CompleteBookingCommand();

            when(controllerMapper.toCompleteCommand(anyLong(), anyLong(), any()))
                    .thenReturn(command);
            when(bookingService.completeBooking(command))
                    .thenThrow(new BusinessException(BookingErrorCode.TOO_MANY_PHOTOS));

            mockMvc.perform(multipart("/api/driver/bookings/{bookingId}/complete", BookingFixture.BOOKING_ID)
                            .file(photo)
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BOOK-026"));
        }
    }

    // ── GET /api/driver/bookings/{bookingId}/vehicle-location ─────────

    @Nested
    @DisplayName("GET /api/driver/bookings/{bookingId}/vehicle-location")
    class GetVehicleLocation {

        @Test
        @DisplayName("Within 2h window - returns masked location")
        void getVehicleLocation_masked_returns200() throws Exception {
            GetBookingCommand command = BookingFixture.getBookingCommandForDriver();
            LocationResult locationResult = BookingFixture.maskedLocationResult();
            VehicleLocationResponse response = BookingFixture.maskedLocationResponse();

            when(controllerMapper.toGetCommand(BookingFixture.BOOKING_ID, BookingFixture.DRIVER_ID))
                    .thenReturn(command);
            when(bookingService.getVehicleLocation(command)).thenReturn(locationResult);
            when(controllerMapper.toVehicleLocationResponse(locationResult)).thenReturn(response);

            mockMvc.perform(get("/api/driver/bookings/{bookingId}/vehicle-location", BookingFixture.BOOKING_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isExactLocation").value(false));
        }

        @Test
        @DisplayName("Within reveal window - returns exact location")
        void getVehicleLocation_exact_returns200() throws Exception {
            GetBookingCommand command = BookingFixture.getBookingCommandForDriver();
            LocationResult locationResult = BookingFixture.exactLocationResult();
            VehicleLocationResponse response = BookingFixture.exactLocationResponse();

            when(controllerMapper.toGetCommand(BookingFixture.BOOKING_ID, BookingFixture.DRIVER_ID))
                    .thenReturn(command);
            when(bookingService.getVehicleLocation(command)).thenReturn(locationResult);
            when(controllerMapper.toVehicleLocationResponse(locationResult)).thenReturn(response);

            mockMvc.perform(get("/api/driver/bookings/{bookingId}/vehicle-location", BookingFixture.BOOKING_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isExactLocation").value(true));
        }

        @Test
        @DisplayName("Booking not found - returns 404 with BOOK-011")
        void getVehicleLocation_notFound_returns404() throws Exception {
            GetBookingCommand command = new GetBookingCommand();
            when(controllerMapper.toGetCommand(999L, BookingFixture.DRIVER_ID)).thenReturn(command);
            when(bookingService.getVehicleLocation(command))
                    .thenThrow(new BusinessException(BookingErrorCode.BOOKING_NOT_FOUND));

            mockMvc.perform(get("/api/driver/bookings/{bookingId}/vehicle-location", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("BOOK-011"));
        }
    }
}
