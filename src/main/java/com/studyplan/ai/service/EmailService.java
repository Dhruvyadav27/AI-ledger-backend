package com.studyplan.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around Spring's JavaMailSender. Kept separate from
 * AuthService so email-sending logic (and any future template/HTML
 * changes) stays in one place.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Reset your StudyPlan AI password");
        message.setText("""
                Hi,

                We received a request to reset your StudyPlan AI password.
                Click the link below to set a new password. This link expires in 15 minutes.

                %s

                If you didn't request this, you can safely ignore this email.
                """.formatted(resetLink));

        try {
            mailSender.send(message);
        } catch (Exception e) {
            // Don't let email failures crash the request - forgotPassword()
            // in AuthService already returns a generic success message
            // regardless (so we don't leak whether an email exists), and
            // logs are where a real failure gets noticed.
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }
}