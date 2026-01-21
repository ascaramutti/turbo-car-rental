package com.turbo.email.service.impl;

import com.turbo.email.service.EmailService;
import com.turbo.exception.BusinessException;
import com.turbo.exception.error.EmailErrorCode;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private static final String PLACEHOLDER_FIRST_NAME = "{{firstName}}";
    private static final String PLACEHOLDER_OTP = "{{otp}}";
    private static final String PLACEHOLDER_EXPIRY = "{{expiryMinutes}}";

    private final JavaMailSender mailSender;

    @Value("${otp.expiry-minutes}")
    private int otpExpiryMinutes;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Value("${app.mail.subject.verification}")
    private String verificationSubject;

    @Value("${app.mail.encoding}")
    private String mailEncoding;

    @Value("${app.mail.template.verification}")
    private String verificationTemplatePath;

    private String verificationTemplate;

    /** Loads the email HTML template from classpath on startup. */
    @PostConstruct
    void loadTemplates() {
        try {
            ClassPathResource resource = new ClassPathResource(verificationTemplatePath);
            verificationTemplate = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to load email template: {}", e.getMessage());
            throw new IllegalStateException("Could not load email template: " + verificationTemplatePath, e);
        }
    }

    @Override
    public void sendVerificationEmail(String to, String firstName, String otp) {
        try {
            MimeMessage message = buildMimeMessage(to, firstName, otp);
            mailSender.send(message);
            log.debug("Verification email sent to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}: {}", to, e.getMessage());
            throw new BusinessException(EmailErrorCode.EMAIL_SEND_FAILED);
        }
    }

    /** Creates and configures the MimeMessage with sender, recipient, subject, and HTML body. */
    private MimeMessage buildMimeMessage(String to, String firstName, String otp) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, mailEncoding);

        helper.setFrom(mailFrom);
        helper.setTo(to);
        helper.setSubject(verificationSubject);
        helper.setText(buildVerificationHtml(firstName, otp), true);

        return message;
    }

    /** Replaces the template placeholders with the actual values. */
    private String buildVerificationHtml(String firstName, String otp) {
        return verificationTemplate
                .replace(PLACEHOLDER_FIRST_NAME, firstName)
                .replace(PLACEHOLDER_OTP, otp)
                .replace(PLACEHOLDER_EXPIRY, String.valueOf(otpExpiryMinutes));
    }
}
