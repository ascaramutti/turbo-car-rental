package com.turbo.booking.service.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelBookingCommand {

    private Long bookingId;
    private Long userId;
    private String userRole;
    private String reason;
}
