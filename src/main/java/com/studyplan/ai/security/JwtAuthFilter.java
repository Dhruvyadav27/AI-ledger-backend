package com.studyplan.ai.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Runs on every incoming request BEFORE it reaches a controller.
 *
 * Flow:
 *  1. Read "Authorization: Bearer <token>" header.
 *  2. If missing -> just let the request continue (SecurityConfig will
 *     reject it later if the endpoint actually requires auth).
 *  3. If present -> validate it. If valid, extract userId and put it into
 *     Spring Security's context as the "authenticated principal" for this
 *     request. Every @RestController can now read it via
 *     SecurityContextHolder.getContext().getAuthentication().getPrincipal()
 *     (we wrap this in a small helper - see CurrentUser below controllers
 *     use it).
 *  4. If invalid/expired -> leave context empty; the request will be
 *     rejected as 401 by SecurityConfig since no authentication was set.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtil.isTokenValid(token)) {
                String userId = jwtUtil.extractUserId(token);

                // principal = userId (a plain String). No roles/authorities
                // needed for this app - every logged-in user has the same
                // permissions, just scoped to their own data.
                var authToken = new UsernamePasswordAuthenticationToken(
                        userId, null, Collections.emptyList());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
