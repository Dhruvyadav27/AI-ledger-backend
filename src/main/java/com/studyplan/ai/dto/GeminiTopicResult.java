package com.studyplan.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal only - never sent to the frontend. This is the shape we ask
 * Gemini to return for EACH topic:
 *   {"title": "ER Model Basics", "order": 1, "weight": 2}
 * GeminiService parses Gemini's raw JSON text response into a List of
 * these before handing off to ScheduleBuilderService.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeminiTopicResult {
    private String title;
    private int order;
    private int weight;
}
