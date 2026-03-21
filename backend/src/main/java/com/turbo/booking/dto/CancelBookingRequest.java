package com.turbo.booking.dto;

import com.turbo.booking.validation.BookingValidationConstraints;
import com.turbo.booking.validation.BookingValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelBookingRequest {

    @NotBlank(message = BookingValidationMessages.REASON_REQUIRED)
    @Size(max = BookingValidationConstraints.MAX_REASON_LENGTH, message = BookingValidationMessages.REASON_SIZE)
    @Pattern(regexp = BookingValidationConstraints.REASON_PATTERN, message = BookingValidationMessages.REASON_PATTERN_MSG)
    private String reason;
}
