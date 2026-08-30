package com.studyplan.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Matches GET /api/streak expected shape:
 * { currentStreak, longestStreak, lastActiveDate }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StreakDto {
    private int currentStreak;
    private int longestStreak;
    private LocalDate lastActiveDate;
}