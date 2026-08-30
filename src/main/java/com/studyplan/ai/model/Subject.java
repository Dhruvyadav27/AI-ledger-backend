package com.studyplan.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Maps to the "subjects" collection from the spec. userId is indexed
 * since almost every query filters by it (data isolation between users).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "subjects")
public class Subject {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String title;
    private String mode;                 // "DEADLINE" | "PACE"
    private Integer totalDays;            // set only for DEADLINE mode
    private Integer dailyTopicCount;      // set only for PACE mode
    private String rescheduleStrategy;    // "PUSH" | "REDISTRIBUTE"

    private LocalDate startDate;
    private LocalDate endDate;
    private String status;                // "ACTIVE" | "COMPLETED"

    private List<Topic> topics;
    private List<ScheduleDay> schedule;

    @CreatedDate
    private Instant createdAt;
}
