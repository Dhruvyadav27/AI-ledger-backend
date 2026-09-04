package com.studyplan.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateTopicTitleRequest {

    @NotBlank(message = "Title is required")
    private String title;
}
