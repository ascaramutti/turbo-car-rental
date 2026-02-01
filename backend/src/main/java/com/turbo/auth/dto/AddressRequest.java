package com.turbo.auth.dto;

import com.turbo.auth.validation.ValidationConstraints;
import com.turbo.auth.validation.ValidationMessages;
import com.turbo.auth.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddressRequest {

    @NotBlank(message = ValidationMessages.STREET_ADDRESS_REQUIRED)
    @Size(max = ValidationConstraints.STREET_ADDRESS_MAX, message = ValidationMessages.STREET_ADDRESS_MAX_LENGTH)
    @Pattern(regexp = ValidationPatterns.STREET_ADDRESS, message = ValidationMessages.STREET_ADDRESS_PATTERN)
    private String streetAddress;

    @NotBlank(message = ValidationMessages.CITY_REQUIRED)
    @Size(max = ValidationConstraints.CITY_MAX, message = ValidationMessages.CITY_MAX_LENGTH)
    @Pattern(regexp = ValidationPatterns.CITY, message = ValidationMessages.CITY_PATTERN)
    private String city;

    @NotBlank(message = ValidationMessages.PROVINCE_REQUIRED)
    @Size(max = ValidationConstraints.PROVINCE_MAX, message = ValidationMessages.PROVINCE_MAX_LENGTH)
    @Pattern(regexp = ValidationPatterns.CITY, message = ValidationMessages.PROVINCE_PATTERN)
    private String province;

    @NotBlank(message = ValidationMessages.POSTAL_CODE_REQUIRED)
    @Pattern(regexp = ValidationPatterns.POSTAL_CODE, message = ValidationMessages.POSTAL_CODE_PATTERN)
    private String postalCode;

    @NotBlank(message = ValidationMessages.COUNTRY_REQUIRED)
    @Size(max = ValidationConstraints.COUNTRY_MAX, message = ValidationMessages.COUNTRY_MAX_LENGTH)
    @Pattern(regexp = ValidationPatterns.CITY, message = ValidationMessages.COUNTRY_PATTERN)
    private String country;
}
