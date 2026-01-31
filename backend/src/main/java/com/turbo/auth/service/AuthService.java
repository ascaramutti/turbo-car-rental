package com.turbo.auth.service;

import com.turbo.auth.dto.AuthResponse;
import com.turbo.auth.dto.MessageResponse;
import com.turbo.auth.dto.RegisterResponse;
import com.turbo.auth.service.command.LoginCommand;
import com.turbo.auth.service.command.RegisterCommand;

public interface AuthService {

    RegisterResponse register(RegisterCommand command);

    AuthResponse verifyOtp(String email, String otp);

    MessageResponse resendOtp(String email);

    AuthResponse login(LoginCommand command);
}
