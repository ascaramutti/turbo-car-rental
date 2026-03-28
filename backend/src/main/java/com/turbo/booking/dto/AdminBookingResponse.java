package com.turbo.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminBookingResponse extends BookingResponse {

    private Long ownerId;
    private String ownerFullName;
    private String vehicleLicensePlate;
}
