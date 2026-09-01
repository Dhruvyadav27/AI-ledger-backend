package com.studyplan.ai.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Matches AuthContext.jsx:
 * api.post("/api/auth/login", { email, password })
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
