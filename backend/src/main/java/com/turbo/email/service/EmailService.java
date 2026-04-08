package com.turbo.email.service;

public interface EmailService {

    void sendVerificationEmail(String to, String firstName, String otp);
}
