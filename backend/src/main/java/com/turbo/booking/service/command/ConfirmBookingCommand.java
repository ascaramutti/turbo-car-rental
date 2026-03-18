package com.turbo.booking.service.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmBookingCommand {

    private Long bookingId;
    private Long ownerId;
}
