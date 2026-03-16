package com.turbo.vehicle.service.command;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class ReuploadVehicleDocumentCommand {

    private Long documentId;
    private Long vehicleId;
    private Long ownerId;
    private MultipartFile file;
}
