package com.turbo.email.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailServiceImpl")
class EmailServiceImplTest {

    @Mock private JavaMailSender mailSender;
    @Mock private MimeMessage mimeMessage;

    @InjectMocks private EmailServiceImpl emailService;

    private static final String TO = "driver@test.com";
    private static final String FIRST_NAME = "John";
    private static final String OTP = "123456";
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final String MAIL_FROM = "noreply@turbo.com";
    private static final String VERIFICATION_SUBJECT = "TURBO - Verify Your Email";
    private static final String MAIL_ENCODING = "UTF-8";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "otpExpiryMinutes", OTP_EXPIRY_MINUTES);
        ReflectionTestUtils.setField(emailService, "mailFrom", MAIL_FROM);
        ReflectionTestUtils.setField(emailService, "verificationSubject", VERIFICATION_SUBJECT);
        ReflectionTestUtils.setField(emailService, "mailEncoding", MAIL_ENCODING);
        ReflectionTestUtils.setField(emailService, "verificationTemplatePath", "email/verification-email.html");
        emailService.loadTemplates();
    }

    // ── Happy path ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy paths")
    class HappyPaths {

        @Test
        @DisplayName("Valid parameters - sends email successfully")
        void sendVerificationEmail_validParams_sendsEmail() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            emailService.sendVerificationEmail(TO, FIRST_NAME, OTP);

            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }
    }

    // ── Verifications ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Verifications")
    class Verifications {

        @Test
        @DisplayName("MimeMessage is created and sent via mailSender")
        void sendVerificationEmail_createsMimeMessageAndSends() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            emailService.sendVerificationEmail(TO, FIRST_NAME, OTP);

            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
            verifyNoMoreInteractions(mailSender);
        }
    }

    // ── Unhappy paths ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Unhappy paths")
    class UnhappyPaths {

        @Test
        @DisplayName("MessagingException - throws BusinessException with EMAIL-001")
        void sendVerificationEmail_messagingException_throwsEmailSendFailed() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new RuntimeException(new MessagingException("SMTP error")))
                    .when(mailSender).send(any(MimeMessage.class));

            assertThatThrownBy(() -> emailService.sendVerificationEmail(TO, FIRST_NAME, OTP))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("createMimeMessage throws - propagates exception")
        void sendVerificationEmail_createMimeMessageFails_throwsException() {
            when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Connection refused"));

            assertThatThrownBy(() -> emailService.sendVerificationEmail(TO, FIRST_NAME, OTP))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
