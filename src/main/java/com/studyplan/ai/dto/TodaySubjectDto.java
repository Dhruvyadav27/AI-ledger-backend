package com.studyplan.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Matches Dashboard.jsx's expected shape exactly:
 * [{ subjectId, subjectTitle, dayNumber, topics: [...] }]
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodaySubjectDto {
    private String subjectId;
    private String subjectTitle;
    private int dayNumber;
    private List<TopicDto> topics;
}
