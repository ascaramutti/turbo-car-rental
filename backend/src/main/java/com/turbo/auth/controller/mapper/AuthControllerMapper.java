package com.turbo.auth.controller.mapper;

import com.turbo.auth.dto.LoginRequest;
import com.turbo.auth.dto.RegisterRequest;
import com.turbo.auth.service.command.LoginCommand;
import com.turbo.auth.service.command.RegisterCommand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthControllerMapper {

    RegisterCommand toRegisterCommand(RegisterRequest request);

    LoginCommand toLoginCommand(LoginRequest request);
}
