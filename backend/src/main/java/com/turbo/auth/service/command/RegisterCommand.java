package com.turbo.auth.service.command;

import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterCommand {

    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private String role;
}
