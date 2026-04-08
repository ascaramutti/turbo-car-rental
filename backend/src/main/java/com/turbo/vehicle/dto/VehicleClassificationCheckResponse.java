package com.turbo.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class VehicleClassificationCheckResponse {

    private boolean readyForClassification;
    private Long vehicleId;
    private Integer vehicleYear;
    private String vehicleMake;
    private String vehicleModel;
    private boolean hasApprovedInspection;
    private List<String> availableServiceTypes;
    private List<VehicleDocumentInfo> documents;

    /** Summary of a vehicle document for the classification modal. */
    @Data
    @AllArgsConstructor
    public static class VehicleDocumentInfo {
        private Long documentId;
        private String documentType;
        private String status;
        private String fileName;
    }
}
