package com.turbo.booking.controller;

import com.turbo.booking.controller.mapper.OwnerDashboardControllerMapper;
import com.turbo.booking.dto.OwnerDashboardResponse;
import com.turbo.booking.fixture.BookingFixture;
import com.turbo.booking.service.BookingService;
import com.turbo.booking.service.result.OwnerDashboardStats;
import com.turbo.config.SecurityHelper;
import com.turbo.exception.GlobalExceptionHandler;
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

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("OwnerDashboardController")
class OwnerDashboardControllerTest {

    @Mock private BookingService bookingService;
    @Mock private OwnerDashboardControllerMapper controllerMapper;
    @Mock private SecurityHelper securityHelper;

    @InjectMocks private OwnerDashboardController ownerDashboardController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ownerDashboardController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        lenient().when(securityHelper.getCurrentUser()).thenReturn(BookingFixture.testCarOwner());
    }

    // ── GET /api/owner/dashboard ──────────────────────────────────────

    @Nested
    @DisplayName("GET /api/owner/dashboard")
    class GetDashboardStats {

        @Test
        @DisplayName("Owner with data - returns 200 with all dashboard fields")
        void getDashboardStats_withData_returns200() throws Exception {
            OwnerDashboardStats stats = BookingFixture.ownerDashboardStats();
            OwnerDashboardResponse response = new OwnerDashboardResponse(
                    3, java.math.BigDecimal.valueOf(960.00), java.math.BigDecimal.valueOf(120.00), 8, 4.8f);

            when(bookingService.getOwnerDashboardStats(BookingFixture.OWNER_ID)).thenReturn(stats);
            when(controllerMapper.toOwnerDashboardResponse(stats)).thenReturn(response);

            mockMvc.perform(get("/api/owner/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activeVehicles").value(3))
                    .andExpect(jsonPath("$.totalEarnings").value(960.00))
                    .andExpect(jsonPath("$.monthEarnings").value(120.00))
                    .andExpect(jsonPath("$.completedBookings").value(8))
                    .andExpect(jsonPath("$.rating").value(4.8f));
        }

        @Test
        @DisplayName("New owner with no activity - returns 200 with zeros and null rating")
        void getDashboardStats_newOwner_returns200WithZeros() throws Exception {
            OwnerDashboardStats stats = new OwnerDashboardStats(
                    0, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, 0, null);
            OwnerDashboardResponse response = new OwnerDashboardResponse(
                    0, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, 0, null);

            when(bookingService.getOwnerDashboardStats(BookingFixture.OWNER_ID)).thenReturn(stats);
            when(controllerMapper.toOwnerDashboardResponse(stats)).thenReturn(response);

            mockMvc.perform(get("/api/owner/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activeVehicles").value(0))
                    .andExpect(jsonPath("$.completedBookings").value(0))
                    .andExpect(jsonPath("$.rating").doesNotExist());
        }
    }
}
