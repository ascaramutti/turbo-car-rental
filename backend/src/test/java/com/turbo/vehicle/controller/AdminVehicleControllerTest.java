package com.turbo.vehicle.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turbo.exception.BusinessException;
import com.turbo.exception.GlobalExceptionHandler;
import com.turbo.exception.error.VehicleErrorCode;
import com.turbo.vehicle.controller.mapper.AdminVehicleControllerMapper;
import com.turbo.vehicle.dto.ApproveVehicleRequest;
import com.turbo.vehicle.dto.VehicleClassificationCheckResponse;
import com.turbo.vehicle.dto.VehicleResponse;
import com.turbo.vehicle.fixture.VehicleFixture;
import com.turbo.vehicle.model.Vehicle;
import com.turbo.vehicle.service.VehicleService;
import com.turbo.vehicle.service.command.ApproveVehicleCommand;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminVehicleController")
class AdminVehicleControllerTest {

    @Mock private VehicleService vehicleService;
    @Mock private AdminVehicleControllerMapper controllerMapper;

    @InjectMocks private AdminVehicleController adminVehicleController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminVehicleController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /** GET /api/admin/vehicles/{id}/classification-check */

    @Nested
    @DisplayName("GET /api/admin/vehicles/{id}/classification-check")
    class ClassificationCheck {

        @Test
        @DisplayName("Valid vehicle - returns 200 with classification data")
        void classificationCheck_validVehicle_returns200() throws Exception {
            VehicleClassificationCheckResponse response = new VehicleClassificationCheckResponse(
                    true,
                    VehicleFixture.VEHICLE_ID,
                    2022,
                    "Toyota",
                    "Corolla",
                    true,
                    List.of("TAXI_AND_DELIVERY", "DELIVERY_ONLY"),
                    List.of()
            );

            when(vehicleService.checkClassificationReadiness(VehicleFixture.VEHICLE_ID))
                    .thenReturn(response);

            mockMvc.perform(get("/api/admin/vehicles/{id}/classification-check", VehicleFixture.VEHICLE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.readyForClassification").value(true))
                    .andExpect(jsonPath("$.vehicleId").value(VehicleFixture.VEHICLE_ID))
                    .andExpect(jsonPath("$.vehicleYear").value(2022))
                    .andExpect(jsonPath("$.vehicleMake").value("Toyota"))
                    .andExpect(jsonPath("$.vehicleModel").value("Corolla"))
                    .andExpect(jsonPath("$.hasApprovedInspection").value(true))
                    .andExpect(jsonPath("$.availableServiceTypes[0]").value("TAXI_AND_DELIVERY"))
                    .andExpect(jsonPath("$.availableServiceTypes[1]").value("DELIVERY_ONLY"));
        }

        @Test
        @DisplayName("Vehicle not found - returns 404 with VEH-004")
        void classificationCheck_notFound_returns404() throws Exception {
            when(vehicleService.checkClassificationReadiness(999L))
                    .thenThrow(new BusinessException(VehicleErrorCode.VEHICLE_NOT_FOUND));

            mockMvc.perform(get("/api/admin/vehicles/{id}/classification-check", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("VEH-004"));
        }
    }

    /** PUT /api/admin/vehicles/{id}/approve */

    @Nested
    @DisplayName("PUT /api/admin/vehicles/{id}/approve")
    class ApproveVehicle {

        @Test
        @DisplayName("Valid approve with TAXI_AND_DELIVERY - returns 200")
        void approveVehicle_taxiAndDelivery_returns200() throws Exception {
            ApproveVehicleRequest request = new ApproveVehicleRequest();
            request.setServiceType("TAXI_AND_DELIVERY");

            Vehicle vehicle = VehicleFixture.pendingVehicle();
            VehicleResponse response = VehicleFixture.pendingVehicleResponse();
            response.setStatus("APPROVED");
            response.setServiceType("TAXI_AND_DELIVERY");

            ApproveVehicleCommand command = new ApproveVehicleCommand();
            when(controllerMapper.toApproveCommand(VehicleFixture.VEHICLE_ID, "TAXI_AND_DELIVERY"))
                    .thenReturn(command);
            when(vehicleService.approveVehicle(command))
                    .thenReturn(vehicle);
            when(controllerMapper.toVehicleResponse(vehicle))
                    .thenReturn(response);

            mockMvc.perform(put("/api/admin/vehicles/{id}/approve", VehicleFixture.VEHICLE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"))
                    .andExpect(jsonPath("$.serviceType").value("TAXI_AND_DELIVERY"));
        }

        @Test
        @DisplayName("Valid approve with DELIVERY_ONLY - returns 200")
        void approveVehicle_deliveryOnly_returns200() throws Exception {
            ApproveVehicleRequest request = new ApproveVehicleRequest();
            request.setServiceType("DELIVERY_ONLY");

            Vehicle vehicle = VehicleFixture.pendingVehicle();
            VehicleResponse response = VehicleFixture.pendingVehicleResponse();
            response.setStatus("APPROVED");
            response.setServiceType("DELIVERY_ONLY");

            ApproveVehicleCommand command = new ApproveVehicleCommand();
            when(controllerMapper.toApproveCommand(VehicleFixture.VEHICLE_ID, "DELIVERY_ONLY"))
                    .thenReturn(command);
            when(vehicleService.approveVehicle(command))
                    .thenReturn(vehicle);
            when(controllerMapper.toVehicleResponse(vehicle))
                    .thenReturn(response);

            mockMvc.perform(put("/api/admin/vehicles/{id}/approve", VehicleFixture.VEHICLE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"))
                    .andExpect(jsonPath("$.serviceType").value("DELIVERY_ONLY"));
        }

        @Test
        @DisplayName("Missing serviceType - returns 400 with VALIDATION-001")
        void approveVehicle_missingServiceType_returns400() throws Exception {
            ApproveVehicleRequest request = new ApproveVehicleRequest();

            mockMvc.perform(put("/api/admin/vehicles/{id}/approve", VehicleFixture.VEHICLE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        @Test
        @DisplayName("Invalid serviceType - returns 400 with VALIDATION-001")
        void approveVehicle_invalidServiceType_returns400() throws Exception {
            ApproveVehicleRequest request = new ApproveVehicleRequest();
            request.setServiceType("INVALID_TYPE");

            mockMvc.perform(put("/api/admin/vehicles/{id}/approve", VehicleFixture.VEHICLE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        @Test
        @DisplayName("Vehicle not found - returns 404 with VEH-004")
        void approveVehicle_notFound_returns404() throws Exception {
            ApproveVehicleRequest request = new ApproveVehicleRequest();
            request.setServiceType("TAXI_AND_DELIVERY");

            ApproveVehicleCommand command = new ApproveVehicleCommand();
            when(controllerMapper.toApproveCommand(999L, "TAXI_AND_DELIVERY"))
                    .thenReturn(command);
            when(vehicleService.approveVehicle(command))
                    .thenThrow(new BusinessException(VehicleErrorCode.VEHICLE_NOT_FOUND));

            mockMvc.perform(put("/api/admin/vehicles/{id}/approve", 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("VEH-004"));
        }

        @Test
        @DisplayName("Old vehicle with TAXI_AND_DELIVERY - returns 400 with VEH-013")
        void approveVehicle_oldVehicleTaxi_returns400() throws Exception {
            ApproveVehicleRequest request = new ApproveVehicleRequest();
            request.setServiceType("TAXI_AND_DELIVERY");

            ApproveVehicleCommand command = new ApproveVehicleCommand();
            when(controllerMapper.toApproveCommand(VehicleFixture.VEHICLE_ID, "TAXI_AND_DELIVERY"))
                    .thenReturn(command);
            when(vehicleService.approveVehicle(command))
                    .thenThrow(new BusinessException(VehicleErrorCode.TAXI_AGE_EXCEEDED));

            mockMvc.perform(put("/api/admin/vehicles/{id}/approve", VehicleFixture.VEHICLE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VEH-013"));
        }
    }

}
