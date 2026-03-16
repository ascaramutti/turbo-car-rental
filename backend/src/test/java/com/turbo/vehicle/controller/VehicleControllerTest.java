package com.turbo.vehicle.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.turbo.config.SecurityHelper;
import com.turbo.exception.BusinessException;
import com.turbo.exception.GlobalExceptionHandler;
import com.turbo.exception.error.VehicleErrorCode;
import com.turbo.vehicle.controller.mapper.VehicleControllerMapper;
import com.turbo.vehicle.dto.ActivateVehicleRequest;
import com.turbo.vehicle.dto.RegisterVehicleRequest;
import com.turbo.vehicle.dto.UpdateVehicleRequest;
import com.turbo.vehicle.dto.VehicleResponse;
import com.turbo.vehicle.fixture.VehicleFixture;
import com.turbo.vehicle.model.Vehicle;
import com.turbo.vehicle.service.VehicleService;
import com.turbo.vehicle.service.command.ActivateVehicleCommand;
import com.turbo.vehicle.service.command.DeactivateVehicleCommand;
import com.turbo.vehicle.service.command.GetVehicleCommand;
import com.turbo.vehicle.service.command.RegisterVehicleCommand;
import com.turbo.vehicle.service.command.UpdateVehicleCommand;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("VehicleController")
class VehicleControllerTest {

    @Mock private VehicleService vehicleService;
    @Mock private VehicleControllerMapper controllerMapper;
    @Mock private SecurityHelper securityHelper;

    @InjectMocks private VehicleController vehicleController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(vehicleController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        lenient().when(securityHelper.getCurrentUser()).thenReturn(VehicleFixture.testCarOwner());
    }

    /** POST /api/owner/vehicles */

    @Nested
    @DisplayName("POST /api/owner/vehicles")
    class RegisterVehicle {

        @Test
        @DisplayName("Valid registration - returns 200 with VehicleResponse")
        void registerVehicle_validRequest_returns200() throws Exception {
            RegisterVehicleRequest request = VehicleFixture.registerVehicleRequest();
            RegisterVehicleCommand command = VehicleFixture.registerVehicleCommand();
            Vehicle vehicle = VehicleFixture.pendingVehicle();
            VehicleResponse response = VehicleFixture.pendingVehicleResponse();

            when(controllerMapper.toRegisterCommand(any(RegisterVehicleRequest.class), anyLong()))
                    .thenReturn(command);
            when(vehicleService.registerVehicle(command)).thenReturn(vehicle);
            when(controllerMapper.toVehicleResponse(vehicle)).thenReturn(response);

            mockMvc.perform(post("/api/owner/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.vehicleId").value(VehicleFixture.VEHICLE_ID));

            verify(controllerMapper).toRegisterCommand(any(RegisterVehicleRequest.class), anyLong());
        }

        @Test
        @DisplayName("Missing required fields - returns 400 with VALIDATION-001")
        void registerVehicle_missingFields_returns400() throws Exception {
            RegisterVehicleRequest request = new RegisterVehicleRequest();

            mockMvc.perform(post("/api/owner/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        @Test
        @DisplayName("Empty optional description - returns 200 (not rejected)")
        void registerVehicle_emptyDescription_returns200() throws Exception {
            RegisterVehicleRequest request = VehicleFixture.registerVehicleRequest();
            request.setDescription("");
            RegisterVehicleCommand command = VehicleFixture.registerVehicleCommand();
            Vehicle vehicle = VehicleFixture.pendingVehicle();
            VehicleResponse response = VehicleFixture.pendingVehicleResponse();

            when(controllerMapper.toRegisterCommand(any(RegisterVehicleRequest.class), anyLong()))
                    .thenReturn(command);
            when(vehicleService.registerVehicle(command)).thenReturn(vehicle);
            when(controllerMapper.toVehicleResponse(vehicle)).thenReturn(response);

            mockMvc.perform(post("/api/owner/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Null optional description - returns 200 (not rejected)")
        void registerVehicle_nullDescription_returns200() throws Exception {
            RegisterVehicleRequest request = VehicleFixture.registerVehicleRequest();
            request.setDescription(null);
            RegisterVehicleCommand command = VehicleFixture.registerVehicleCommand();
            Vehicle vehicle = VehicleFixture.pendingVehicle();
            VehicleResponse response = VehicleFixture.pendingVehicleResponse();

            when(controllerMapper.toRegisterCommand(any(RegisterVehicleRequest.class), anyLong()))
                    .thenReturn(command);
            when(vehicleService.registerVehicle(command)).thenReturn(vehicle);
            when(controllerMapper.toVehicleResponse(vehicle)).thenReturn(response);

            mockMvc.perform(post("/api/owner/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    /** GET /api/owner/vehicles */

    @Nested
    @DisplayName("GET /api/owner/vehicles")
    class GetMyVehicles {

        @Test
        @DisplayName("Returns 200 with list of vehicles")
        void getMyVehicles_returns200() throws Exception {
            Vehicle vehicle = VehicleFixture.pendingVehicle();
            VehicleResponse response = VehicleFixture.pendingVehicleResponse();
            when(vehicleService.getMyVehicles(VehicleFixture.OWNER_USER_ID))
                    .thenReturn(List.of(vehicle));
            when(controllerMapper.toVehicleResponseList(List.of(vehicle)))
                    .thenReturn(List.of(response));

            mockMvc.perform(get("/api/owner/vehicles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].vehicleId").value(VehicleFixture.VEHICLE_ID));
        }
    }

    /** GET /api/owner/vehicles/{vehicleId} */

    @Nested
    @DisplayName("GET /api/owner/vehicles/{vehicleId}")
    class GetVehicleById {

        @Test
        @DisplayName("Returns 200 with vehicle")
        void getVehicleById_returns200() throws Exception {
            Vehicle vehicle = VehicleFixture.pendingVehicle();
            VehicleResponse response = VehicleFixture.pendingVehicleResponse();
            GetVehicleCommand command = new GetVehicleCommand();
            when(controllerMapper.toGetVehicleCommand(VehicleFixture.VEHICLE_ID, VehicleFixture.OWNER_USER_ID))
                    .thenReturn(command);
            when(vehicleService.getVehicleById(command))
                    .thenReturn(vehicle);
            when(controllerMapper.toVehicleResponse(vehicle))
                    .thenReturn(response);

            mockMvc.perform(get("/api/owner/vehicles/{vehicleId}", VehicleFixture.VEHICLE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.vehicleId").value(VehicleFixture.VEHICLE_ID));
        }

        @Test
        @DisplayName("Vehicle not found - returns 404 with VEH-004")
        void getVehicleById_notFound_returns404() throws Exception {
            GetVehicleCommand command = new GetVehicleCommand();
            when(controllerMapper.toGetVehicleCommand(999L, VehicleFixture.OWNER_USER_ID))
                    .thenReturn(command);
            when(vehicleService.getVehicleById(command))
                    .thenThrow(new BusinessException(VehicleErrorCode.VEHICLE_NOT_FOUND));

            mockMvc.perform(get("/api/owner/vehicles/{vehicleId}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("VEH-004"));
        }
    }

    /** PUT /api/owner/vehicles/{vehicleId} */

    @Nested
    @DisplayName("PUT /api/owner/vehicles/{vehicleId}")
    class UpdateVehicle {

        @Test
        @DisplayName("Valid update - returns 200")
        void updateVehicle_validRequest_returns200() throws Exception {
            UpdateVehicleRequest request = new UpdateVehicleRequest();
            request.setMake("Honda");

            UpdateVehicleCommand command = new UpdateVehicleCommand();
            Vehicle vehicle = VehicleFixture.pendingVehicle();
            vehicle.setMake("Honda");
            VehicleResponse response = VehicleFixture.pendingVehicleResponse();
            response.setMake("Honda");

            when(controllerMapper.toUpdateCommand(any(UpdateVehicleRequest.class), anyLong(), anyLong()))
                    .thenReturn(command);
            when(vehicleService.updateVehicle(command)).thenReturn(vehicle);
            when(controllerMapper.toVehicleResponse(vehicle)).thenReturn(response);

            mockMvc.perform(put("/api/owner/vehicles/{vehicleId}", VehicleFixture.VEHICLE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.make").value("Honda"));
        }
    }

    /** PUT /api/owner/vehicles/{vehicleId}/activate */

    @Nested
    @DisplayName("PUT /api/owner/vehicles/{vehicleId}/activate")
    class ActivateVehicle {

        @Test
        @DisplayName("Valid activation with availableUntil and location - returns 200")
        void activateVehicle_validRequest_returns200() throws Exception {
            ActivateVehicleRequest request = VehicleFixture.activateVehicleRequest();

            Vehicle vehicle = VehicleFixture.approvedVehicle();
            vehicle.setIsActive(true);
            vehicle.setAvailableUntil(request.getAvailableUntil());
            VehicleResponse response = VehicleFixture.pendingVehicleResponse();
            response.setIsActive(true);
            response.setStatus("APPROVED");
            response.setAvailableUntil(request.getAvailableUntil().toString());

            ActivateVehicleCommand command = new ActivateVehicleCommand();
            when(controllerMapper.toActivateVehicleCommand(
                    eq(VehicleFixture.VEHICLE_ID), eq(VehicleFixture.OWNER_USER_ID), any(ActivateVehicleRequest.class)))
                    .thenReturn(command);
            when(vehicleService.activateVehicle(command))
                    .thenReturn(vehicle);
            when(controllerMapper.toVehicleResponse(vehicle))
                    .thenReturn(response);

            mockMvc.perform(put("/api/owner/vehicles/{vehicleId}/activate", VehicleFixture.VEHICLE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isActive").value(true));
        }

        @Test
        @DisplayName("Vehicle not approved - returns 400 VEH-009")
        void activateVehicle_notApproved_returns400() throws Exception {
            ActivateVehicleRequest request = VehicleFixture.activateVehicleRequest();

            ActivateVehicleCommand command = new ActivateVehicleCommand();
            when(controllerMapper.toActivateVehicleCommand(
                    eq(VehicleFixture.VEHICLE_ID), eq(VehicleFixture.OWNER_USER_ID), any(ActivateVehicleRequest.class)))
                    .thenReturn(command);
            when(vehicleService.activateVehicle(command))
                    .thenThrow(new BusinessException(VehicleErrorCode.VEHICLE_NOT_APPROVED));

            mockMvc.perform(put("/api/owner/vehicles/{vehicleId}/activate", VehicleFixture.VEHICLE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VEH-009"));
        }
    }

    /** PUT /api/owner/vehicles/{vehicleId}/deactivate */

    @Nested
    @DisplayName("PUT /api/owner/vehicles/{vehicleId}/deactivate")
    class DeactivateVehicle {

        @Test
        @DisplayName("Valid deactivation - returns 204")
        void deactivateVehicle_validRequest_returns204() throws Exception {
            DeactivateVehicleCommand command = new DeactivateVehicleCommand();
            when(controllerMapper.toDeactivateVehicleCommand(VehicleFixture.VEHICLE_ID, VehicleFixture.OWNER_USER_ID))
                    .thenReturn(command);

            mockMvc.perform(put("/api/owner/vehicles/{vehicleId}/deactivate", VehicleFixture.VEHICLE_ID))
                    .andExpect(status().isNoContent());

            verify(vehicleService).deactivateVehicle(command);
        }
    }
}
