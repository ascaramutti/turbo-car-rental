package com.turbo.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleLocationResponse {

    private Long vehicleId;
    private Boolean isExactLocation;
    private String generalLocation;
    private Double latitude;
    private Double longitude;
    private String message;
}
