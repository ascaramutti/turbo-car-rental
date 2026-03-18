package com.turbo.booking.service.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetBookingCommand {

    private Long bookingId;
    private Long userId;
    private String userRole;
}
