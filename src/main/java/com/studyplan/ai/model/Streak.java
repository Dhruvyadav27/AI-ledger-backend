package com.studyplan.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Embedded (not a separate collection) inside User, exactly as per the
 * data model: users.streak = { currentStreak, longestStreak, lastActiveDate }.
 * Updated by the streak-calculation logic we add in Phase 4, when a topic
 * is marked complete.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Streak {
    private int currentStreak = 0;
    private int longestStreak = 0;
    private LocalDate lastActiveDate;
}
