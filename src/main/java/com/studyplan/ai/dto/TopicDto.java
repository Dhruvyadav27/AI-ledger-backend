package com.studyplan.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Outgoing shape for a single topic. Field names match Dashboard.jsx's
 * expected shape exactly: {topicId, title, orderIndex, weight, status}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicDto {
    private String topicId;
    private String title;
    private int orderIndex;
    private int weight;
    private String status;
}
