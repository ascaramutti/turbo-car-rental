package com.turbo.booking.service.command;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class CompleteBookingCommand {

    private Long bookingId;
    private Long driverId;
    private List<MultipartFile> returnPhotos;
}
