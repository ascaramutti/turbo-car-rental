package com.turbo.user.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "drivers")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Driver extends User {

    private String licenseNumber;
    private String licenseClass;
    private LocalDate licenseExpiryDate;
    private String studyPermitNumber;
    private LocalDate studyPermitExpiry;
    private Boolean isWorkEligible = false;
    private String checkStatus;
}
