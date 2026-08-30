package com.studyplan.ai.controller;

import com.studyplan.ai.dto.TodaySubjectDto;
import com.studyplan.ai.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/today")
    public List<TodaySubjectDto> today() {
        return dashboardService.getTodayTopics();
    }
}