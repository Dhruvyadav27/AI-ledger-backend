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

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

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
}
