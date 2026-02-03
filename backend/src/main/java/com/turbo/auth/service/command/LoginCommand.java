package com.turbo.auth.service.command;

import lombok.Data;

@Data
public class LoginCommand {

    private String email;
    private String password;
}
