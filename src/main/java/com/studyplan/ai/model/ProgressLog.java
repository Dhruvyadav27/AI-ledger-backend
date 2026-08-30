package com.studyplan.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Maps to the "progress_logs" collection - one document per (user, date)
 * pair, aggregating how many topics were completed that day. This is
 * what powers the heatmap (Phase 4) without having to scan every
 * subject's schedule every time.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "progress_logs")
@CompoundIndex(name = "user_date_idx", def = "{'userId': 1, 'date': 1}", unique = true)
public class ProgressLog {

    @Id
    private String id;

    private String userId;
    private LocalDate date;
    private int topicsCompletedCount;

    @Builder.Default
    private Set<String> subjectsTouched = new HashSet<>();
}