package com.studyplan.ai.service;

import com.studyplan.ai.dto.HeatmapEntryDto;
import com.studyplan.ai.model.ProgressLog;
import com.studyplan.ai.repository.ProgressLogRepository;
import com.studyplan.ai.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Keeps the "progress_logs" collection (one doc per user+date) in sync
 * whenever a topic is completed or un-completed, and reads it back for
 * the heatmap.
 */
@Service
@RequiredArgsConstructor
public class ProgressLogService {

    private final ProgressLogRepository progressLogRepository;
    private final StreakService streakService;

    /** Call on PENDING -> DONE. Increments today's count, updates streak. */
    public void recordCompletion(String subjectId) {
        LocalDate today = LocalDate.now();
        String userId = CurrentUser.id();

        ProgressLog log = progressLogRepository.findByUserIdAndDate(userId, today)
                .orElseGet(() -> ProgressLog.builder()
                        .userId(userId)
                        .date(today)
                        .topicsCompletedCount(0)
                        .build());

        log.setTopicsCompletedCount(log.getTopicsCompletedCount() + 1);
        log.getSubjectsTouched().add(subjectId);
        progressLogRepository.save(log);

        streakService.onTopicCompleted(today);
    }

    /** Call on DONE -> PENDING (undo). Decrements today's count. */
    public void recordUndo(String subjectId) {
        LocalDate today = LocalDate.now();
        String userId = CurrentUser.id();

        progressLogRepository.findByUserIdAndDate(userId, today).ifPresent(log -> {
            log.setTopicsCompletedCount(Math.max(0, log.getTopicsCompletedCount() - 1));
            progressLogRepository.save(log);

            if (log.getTopicsCompletedCount() == 0) {
                streakService.onCompletionUndoneAndDayNowEmpty(today);
            }
        });
    }

    public List<HeatmapEntryDto> getHeatmapForYear(int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        return progressLogRepository.findByUserIdAndDateBetween(CurrentUser.id(), start, end)
                .stream()
                .map(log -> HeatmapEntryDto.builder()
                        .date(log.getDate())
                        .topicsCompletedCount(log.getTopicsCompletedCount())
                        .build())
                .toList();
    }
}