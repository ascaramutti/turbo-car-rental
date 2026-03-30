package com.turbo.booking.dto;

import com.turbo.booking.validation.BookingValidationMessages;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateBookingRequest {

    @NotNull(message = BookingValidationMessages.VEHICLE_ID_REQUIRED)
    @Min(value = 1, message = BookingValidationMessages.VEHICLE_ID_INVALID)
    private Long vehicleId;

    @NotNull(message = BookingValidationMessages.START_TIME_REQUIRED)
    @Future(message = BookingValidationMessages.START_TIME_FUTURE)
    private LocalDateTime startTime;

    @NotNull(message = BookingValidationMessages.END_TIME_REQUIRED)
    @Future(message = BookingValidationMessages.END_TIME_FUTURE)
    private LocalDateTime endTime;
}
