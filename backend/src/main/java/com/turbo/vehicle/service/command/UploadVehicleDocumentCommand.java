package com.turbo.vehicle.service.command;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class UploadVehicleDocumentCommand {

    private Long vehicleId;
    private Long ownerId;
    private MultipartFile file;
    private String documentType;
}
