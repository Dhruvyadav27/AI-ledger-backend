package com.studyplan.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddTopicRequest {

    @NotBlank(message = "Title is required")
    private String title;
}