package com.studyplan.ai.service;

import com.studyplan.ai.dto.AuthResponse;
import com.studyplan.ai.dto.LoginRequest;
import com.studyplan.ai.dto.SignupRequest;
import com.studyplan.ai.exception.ApiException;
import com.studyplan.ai.model.Streak;
import com.studyplan.ai.model.User;
import com.studyplan.ai.repository.UserRepository;
import com.studyplan.ai.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.studyplan.ai.dto.ForgotPasswordRequest;
import com.studyplan.ai.dto.ResetPasswordRequest;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    @Value("${app.reset-token-expiry-ms}")
    private long resetTokenExpiryMs;

    public AuthResponse signup(SignupRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .streak(new Streak()) // currentStreak=0, longestStreak=0, lastActiveDate=null
                .build();

        User saved = userRepository.save(user);

        String token = jwtUtil.generateToken(saved.getId(), saved.getEmail());

        return AuthResponse.builder()
                .token(token)
                .id(saved.getId())
                .name(saved.getName())
                .email(saved.getEmail())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
    /**
     * Always returns success-shaped behavior regardless of whether the
     * email exists - this prevents attackers from using this endpoint to
     * discover which emails are registered (a common security practice).
     */
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setResetPasswordToken(token);
            user.setResetPasswordTokenExpiry(Instant.now().plusMillis(resetTokenExpiryMs));
            userRepository.save(user);

            emailService.sendPasswordResetEmail(user.getEmail(), token);
        });
        // If no user matched, we silently do nothing - caller always sees the same generic message.
    }

    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findAll().stream()
                .filter(u -> request.getToken().equals(u.getResetPasswordToken()))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid or expired reset link"));

        if (user.getResetPasswordTokenExpiry() == null
                || user.getResetPasswordTokenExpiry().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid or expired reset link");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);
    }
}
