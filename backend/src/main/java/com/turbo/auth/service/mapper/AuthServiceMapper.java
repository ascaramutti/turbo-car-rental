package com.turbo.auth.service.mapper;

import com.turbo.auth.dto.AuthResponse;
import com.turbo.auth.dto.MessageResponse;
import com.turbo.auth.dto.RegisterResponse;
import com.turbo.auth.service.command.AddressCommand;
import com.turbo.auth.service.command.RegisterCommand;
import com.turbo.user.model.Address;
import com.turbo.user.model.User;
import com.turbo.user.model.enums.UserRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AuthServiceMapper {

    String REGISTRATION_SUCCESS_MESSAGE = "Registration successful. Please check your email for the verification code.";
    String RESEND_OTP_SUCCESS_MESSAGE = "A new verification code has been sent to your email.";

    @Mapping(source = "user.userId", target = "userId")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.firstName", target = "firstName")
    @Mapping(source = "user.lastName", target = "lastName")
    @Mapping(target = "role", expression = "java(user.getRole().name())")
    @Mapping(source = "user.isVerified", target = "isVerified")
    AuthResponse toAuthResponse(User user, String token);

    @Mapping(source = "email", target = "email")
    @Mapping(target = "message", constant = REGISTRATION_SUCCESS_MESSAGE)
    @Mapping(target = "requiresVerification", constant = "true")
    RegisterResponse toRegisterResponse(User user);

    default MessageResponse toResendOtpResponse() {
        return new MessageResponse(RESEND_OTP_SUCCESS_MESSAGE);
    }

    @Mapping(source = "command.firstName", target = "firstName")
    @Mapping(source = "command.lastName", target = "lastName")
    @Mapping(source = "command.email", target = "email")
    @Mapping(source = "command.phoneNumber", target = "phoneNumber")
    @Mapping(source = "command.dateOfBirth", target = "dateOfBirth")
    @Mapping(source = "command.address", target = "address")
    @Mapping(source = "encodedPassword", target = "password")
    @Mapping(source = "role", target = "role")
    @Mapping(target = "isVerified", expression = "java(false)")
    @Mapping(target = "emailVerified", expression = "java(false)")
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "otpCode", ignore = true)
    @Mapping(target = "otpExpiresAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateUserFromCommand(RegisterCommand command, String encodedPassword, UserRole role, @MappingTarget User user);

    /** Maps the address command to the embeddable entity. */
    Address toAddress(AddressCommand addressCommand);
}
