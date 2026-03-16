package com.turbo.vehicle.service;

import com.turbo.document.model.Document;
import com.turbo.vehicle.service.command.GetVehicleDocumentsCommand;
import com.turbo.vehicle.service.command.ReuploadVehicleDocumentCommand;
import com.turbo.vehicle.service.command.UploadVehicleDocumentCommand;
import com.turbo.vehicle.service.command.ViewVehicleDocumentCommand;

import java.util.List;

public interface VehicleDocumentService {

    Document uploadVehicleDocument(UploadVehicleDocumentCommand command);

    List<Document> getVehicleDocuments(GetVehicleDocumentsCommand command);

    Document reuploadVehicleDocument(ReuploadVehicleDocumentCommand command);

    Document getDocumentForView(ViewVehicleDocumentCommand command);

    Document getDocumentForDownload(ViewVehicleDocumentCommand command);
}
