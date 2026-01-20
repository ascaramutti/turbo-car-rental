package com.turbo.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Embeddable address component shared across entities.
 * Stored as columns in the owning entity's table (no separate table created).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @NotBlank
    @Column(nullable = false)
    private String streetAddress;

    @NotBlank
    @Column(nullable = false)
    private String city;

    @NotBlank
    @Column(nullable = false)
    private String province;

    @NotBlank
    @Column(nullable = false, length = 7)
    private String postalCode;

    @NotBlank
    @Column(nullable = false)
    private String country;
}
