package com.studyplan.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Topic {
    private String topicId;      // e.g. "t1", "t2" - generated when subject is created
    private String title;
    private int orderIndex;      // prerequisite order, from Gemini (1, 2, 3...)
    private int weight;          // difficulty/time estimate 1-5, from Gemini
    private String status;       // "PENDING" | "DONE" | "RESCHEDULED"
}
