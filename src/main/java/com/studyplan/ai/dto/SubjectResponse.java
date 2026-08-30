package com.studyplan.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Full subject shape returned by POST /api/subjects and
 * GET /api/subjects/{id}. NewSubject.jsx reads res.data._id or
 * res.data.id after creation - we include both "id" (our field name)
 * so either works; SubjectDetail.jsx renders topics/schedule from here.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectResponse {
    private String id;
    private String title;
    private String mode;
    private Integer totalDays;
    private Integer dailyTopicCount;
    private String rescheduleStrategy;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private List<TopicDto> topics;
    private List<ScheduleDayDto> schedule;
}
