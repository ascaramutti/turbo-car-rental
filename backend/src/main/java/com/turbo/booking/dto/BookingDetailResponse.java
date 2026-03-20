package com.turbo.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetailResponse extends BookingResponse {

    private Long ownerId;
    private String ownerFullName;
    private String vehicleMake;
    private String vehicleModel;
    private Integer vehicleYear;
    private String vehicleLicensePlate;
    private String vehicleCategory;
    private String vehicleServiceType;
    private BigDecimal vehicleHourlyRate;
    private String pickupLocation;
    private List<String> pickupPhotoUrls;
    private List<String> returnPhotoUrls;
    private String cancellationReason;
    private String cancelledBy;
    private String confirmedAt;
    private String startedAt;
    private String completedAt;
    private String cancelledAt;
    private String effectiveServiceType;
    private String serviceTypeWarning;
}
