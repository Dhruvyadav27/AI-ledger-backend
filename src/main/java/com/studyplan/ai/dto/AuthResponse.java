package com.studyplan.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AuthContext.jsx does:
 *   const { token, ...userInfo } = res.data;
 *   localStorage.setItem("studyplan_user", JSON.stringify(userInfo));
 *
 * So "token" MUST be a top-level sibling field, and everything else here
 * (id, name, email) becomes the stored "userInfo" on the frontend.
 * passwordHash is intentionally never included.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String id;
    private String name;
    private String email;
}
