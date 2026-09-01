package com.studyplan.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point. @EnableScheduling is turned on now because Phase 3's
 * nightly auto-reschedule job (@Scheduled cron) needs it - harmless to
 * enable early.
 */
@SpringBootApplication
@EnableScheduling
public class StudyPlanAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(StudyPlanAiApplication.class, args);
    }
}
