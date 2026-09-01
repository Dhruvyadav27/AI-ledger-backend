package com.studyplan.ai.security;

import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Usage inside any controller method (once an endpoint is behind the JWT
 * filter, e.g. everything except /api/auth/**):
 *
 *   String userId = CurrentUser.id();
 *
 * This is how EVERY future endpoint (subjects, dashboard, streak, heatmap)
 * scopes its MongoDB queries to "userId = CurrentUser.id()" - this is what
 * keeps each user's data fully isolated, as required by the spec.
 */
public final class CurrentUser {

    private CurrentUser() {}

    public static String id() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
