package com.turbo.vehicle.controller;

import com.turbo.vehicle.controller.mapper.AdminVehicleControllerMapper;
import com.turbo.vehicle.dto.ApproveVehicleRequest;
import com.turbo.vehicle.dto.VehicleClassificationCheckResponse;
import com.turbo.vehicle.dto.VehicleResponse;
import com.turbo.vehicle.model.Vehicle;
import com.turbo.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/vehicles")
@RequiredArgsConstructor
public class AdminVehicleController {

    private final VehicleService vehicleService;
    private final AdminVehicleControllerMapper controllerMapper;

    @GetMapping("/{id}/classification-check")
    public ResponseEntity<VehicleClassificationCheckResponse> checkClassification(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.checkClassificationReadiness(id));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<VehicleResponse> approveVehicle(
            @PathVariable Long id,
            @Valid @RequestBody ApproveVehicleRequest request) {

        Vehicle vehicle = vehicleService.approveVehicle(
                controllerMapper.toApproveCommand(id, request.getServiceType()));
        return ResponseEntity.ok(controllerMapper.toVehicleResponse(vehicle));
    }

}
