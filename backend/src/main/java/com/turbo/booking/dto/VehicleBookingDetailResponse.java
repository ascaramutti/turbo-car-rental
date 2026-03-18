package com.turbo.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleBookingDetailResponse extends VehicleSearchResponse {

    private String licensePlate;
    private Long ownerId;
    private String vin;
}
