package com.studyplan.ai.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Frontend sends the FULL new order of topicIds after a drag/arrow-move.
 * Simplest and most reliable contract - backend just re-indexes
 * orderIndex to match this exact sequence, no fragile position math.
 */
@Data
public class ReorderTopicsRequest {

    @NotEmpty(message = "topicIds is required")
    private List<String> topicIds;
}