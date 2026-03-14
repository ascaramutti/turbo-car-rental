package com.turbo.user.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "car_owners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CarOwner extends User {

    private String bankAccountNumber;
    private String bankName;
    private Float rating = 0.0f;
    private BigDecimal totalEarnings = BigDecimal.ZERO;
}
