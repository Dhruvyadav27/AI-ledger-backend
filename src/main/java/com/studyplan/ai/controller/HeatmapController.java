package com.studyplan.ai.controller;

import com.studyplan.ai.dto.HeatmapEntryDto;
import com.studyplan.ai.service.ProgressLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class HeatmapController {

    private final ProgressLogService progressLogService;

    @GetMapping("/api/heatmap")
    public List<HeatmapEntryDto> heatmap(@RequestParam int year) {
        return progressLogService.getHeatmapForYear(year);
    }
}