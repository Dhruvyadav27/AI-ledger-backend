package com.studyplan.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * Matches exactly what NewSubject.jsx sends:
 * { title, mode, rescheduleStrategy, rawTopics[], totalDays | dailyTopicCount }
 *
 * totalDays and dailyTopicCount are both nullable here since only one of
 * them arrives depending on "mode" - SubjectService validates which one
 * is actually required based on mode.
 */
@Data
public class CreateSubjectRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @Pattern(regexp = "DEADLINE|PACE", message = "Mode must be DEADLINE or PACE")
    private String mode;

    @Pattern(regexp = "PUSH|REDISTRIBUTE", message = "Reschedule strategy must be PUSH or REDISTRIBUTE")
    private String rescheduleStrategy;

    @NotEmpty(message = "Add at least one topic")
    private List<String> rawTopics;

    private Integer totalDays;         // required only when mode = DEADLINE
    private Integer dailyTopicCount;   // required only when mode = PACE
}