package com.studyplan.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Matches HeatmapPage.jsx's expected shape exactly:
 * [{ date: "YYYY-MM-DD", topicsCompletedCount }]
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeatmapEntryDto {
    private LocalDate date;
    private int topicsCompletedCount;
}
