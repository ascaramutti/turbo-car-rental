package com.turbo.vehicle.controller;

import com.turbo.config.SecurityHelper;
import com.turbo.user.model.User;
import com.turbo.vehicle.controller.mapper.VehicleControllerMapper;
import com.turbo.vehicle.dto.ActivateVehicleRequest;
import com.turbo.vehicle.dto.RegisterVehicleRequest;
import com.turbo.vehicle.dto.UpdateVehicleRequest;
import com.turbo.vehicle.dto.VehicleResponse;
import com.turbo.vehicle.model.Vehicle;
import com.turbo.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/owner/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final VehicleControllerMapper controllerMapper;
    private final SecurityHelper securityHelper;

    @PostMapping
    public ResponseEntity<VehicleResponse> registerVehicle(@Valid @RequestBody RegisterVehicleRequest request) {
        User user = securityHelper.getCurrentUser();
        Vehicle vehicle = vehicleService.registerVehicle(
                controllerMapper.toRegisterCommand(request, user.getUserId()));
        return ResponseEntity.ok(controllerMapper.toVehicleResponse(vehicle));
    }

    @GetMapping
    public ResponseEntity<List<VehicleResponse>> getMyVehicles() {
        User user = securityHelper.getCurrentUser();
        List<Vehicle> vehicles = vehicleService.getMyVehicles(user.getUserId());
        return ResponseEntity.ok(controllerMapper.toVehicleResponseList(vehicles));
    }

    @GetMapping("/{vehicleId}")
    public ResponseEntity<VehicleResponse> getVehicleById(@PathVariable Long vehicleId) {
        User user = securityHelper.getCurrentUser();
        Vehicle vehicle = vehicleService.getVehicleById(
                controllerMapper.toGetVehicleCommand(vehicleId, user.getUserId()));
        return ResponseEntity.ok(controllerMapper.toVehicleResponse(vehicle));
    }

    @PutMapping("/{vehicleId}")
    public ResponseEntity<VehicleResponse> updateVehicle(
            @PathVariable Long vehicleId,
            @Valid @RequestBody UpdateVehicleRequest request) {

        User user = securityHelper.getCurrentUser();
        Vehicle vehicle = vehicleService.updateVehicle(
                controllerMapper.toUpdateCommand(request, vehicleId, user.getUserId()));
        return ResponseEntity.ok(controllerMapper.toVehicleResponse(vehicle));
    }

    @PutMapping("/{vehicleId}/activate")
    public ResponseEntity<VehicleResponse> activateVehicle(
            @PathVariable Long vehicleId,
            @Valid @RequestBody ActivateVehicleRequest request) {

        User user = securityHelper.getCurrentUser();
        Vehicle vehicle = vehicleService.activateVehicle(
                controllerMapper.toActivateVehicleCommand(vehicleId, user.getUserId(), request));
        return ResponseEntity.ok(controllerMapper.toVehicleResponse(vehicle));
    }

    @PutMapping("/{vehicleId}/deactivate")
    public ResponseEntity<Void> deactivateVehicle(@PathVariable Long vehicleId) {
        User user = securityHelper.getCurrentUser();
        vehicleService.deactivateVehicle(
                controllerMapper.toDeactivateVehicleCommand(vehicleId, user.getUserId()));
        return ResponseEntity.noContent().build();
    }
}
