package com.studyplan.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleDayDto {
    private int dayNumber;
    private LocalDate date;
    private List<String> topicIds;
    private boolean completed;
}
