package com.studyplan.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDay {
    private int dayNumber;           // 1, 2, 3...
    private LocalDate date;
    private List<String> topicIds;
    private boolean completed;       // true once every topic in this day is DONE
}
