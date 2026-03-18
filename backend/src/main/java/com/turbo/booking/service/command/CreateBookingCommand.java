package com.turbo.booking.service.command;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateBookingCommand {

    private Long driverId;
    private Long vehicleId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
