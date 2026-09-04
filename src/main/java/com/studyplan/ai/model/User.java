package com.studyplan.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Maps directly to the "users" collection from the spec:
 * { _id, name, email, passwordHash, createdAt, streak: {...} }
 *
 * Never returned directly from a controller - AuthController maps this
 * to an AuthResponse DTO so passwordHash never leaves the server.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String name;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    @CreatedDate
    private Instant createdAt;

    @Builder.Default
    private Streak streak = new Streak();


    // Set when forgot-password is requested, cleared once used or expired.
    private String resetPasswordToken;
    private Instant resetPasswordTokenExpiry;
}
