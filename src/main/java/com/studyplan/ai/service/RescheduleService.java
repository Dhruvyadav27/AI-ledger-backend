package com.studyplan.ai.service;

import com.studyplan.ai.model.ScheduleDay;
import com.studyplan.ai.model.Subject;
import com.studyplan.ai.model.Topic;
import com.studyplan.ai.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Runs every night at midnight (cron = "0 0 0 * * *"). For every ACTIVE
 * subject, finds days in the past (date < today) that still have
 * PENDING topics, and reschedules those topics per the subject's
 * chosen strategy:
 *
 *  PUSH         - move pending topics into tomorrow's slot. Works for
 *                 both modes - in PACE mode this just grows the end
 *                 date naturally.
 *  REDISTRIBUTE - DEADLINE mode only (enforced at creation time) -
 *                 evenly re-spreads all still-pending topics across
 *                 the days remaining before the subject's endDate.
 *
 * Either way, every moved topic's status flips to "RESCHEDULED" per
 * the spec.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RescheduleService {

    private final SubjectRepository subjectRepository;

    @Scheduled(cron = "0 0 0 * * *")
    public void runNightlyReschedule() {
        LocalDate today = LocalDate.now();
        List<Subject> activeSubjects = subjectRepository.findAll().stream()
                .filter(s -> "ACTIVE".equals(s.getStatus()))
                .toList();

        for (Subject subject : activeSubjects) {
            try {
                if ("REDISTRIBUTE".equals(subject.getRescheduleStrategy())) {
                    redistribute(subject, today);
                } else {
                    push(subject, today);
                }
                subjectRepository.save(subject);
            } catch (Exception e) {
                log.error("Reschedule failed for subject {}: {}", subject.getId(), e.getMessage());
            }
        }
    }

    // ---------- PUSH ----------
    private void push(Subject subject, LocalDate today) {
        List<String> pendingTopicIds = collectOverduePendingTopicIds(subject, today);
        if (pendingTopicIds.isEmpty()) return;

        markRescheduled(subject, pendingTopicIds);
        removeFromOverdueDays(subject, today, pendingTopicIds);

        LocalDate tomorrow = today.plusDays(1);
        ScheduleDay tomorrowDay = subject.getSchedule().stream()
                .filter(d -> tomorrow.equals(d.getDate()))
                .findFirst()
                .orElse(null);

        if (tomorrowDay != null) {
            tomorrowDay.getTopicIds().addAll(pendingTopicIds);
        } else {
            int nextDayNumber = subject.getSchedule().stream()
                    .mapToInt(ScheduleDay::getDayNumber).max().orElse(0) + 1;
            subject.getSchedule().add(
                    new ScheduleDay(nextDayNumber, tomorrow, new ArrayList<>(pendingTopicIds), false));
        }

        LocalDate maxDate = subject.getSchedule().stream()
                .map(ScheduleDay::getDate).max(LocalDate::compareTo).orElse(subject.getEndDate());
        subject.setEndDate(maxDate);
    }

    // ---------- REDISTRIBUTE (DEADLINE mode only) ----------
    private void redistribute(Subject subject, LocalDate today) {
        List<String> pendingTopicIds = collectOverduePendingTopicIds(subject, today);
        if (pendingTopicIds.isEmpty()) return;

        markRescheduled(subject, pendingTopicIds);
        removeFromOverdueDays(subject, today, pendingTopicIds);

        List<ScheduleDay> futureDays = subject.getSchedule().stream()
                .filter(d -> !d.getDate().isBefore(today))
                .sorted(Comparator.comparing(ScheduleDay::getDate))
                .toList();

        if (futureDays.isEmpty()) {
            int nextDayNumber = subject.getSchedule().stream()
                    .mapToInt(ScheduleDay::getDayNumber).max().orElse(0) + 1;
            LocalDate day = subject.getEndDate() != null ? subject.getEndDate().plusDays(1) : today;
            subject.getSchedule().add(new ScheduleDay(nextDayNumber, day, new ArrayList<>(pendingTopicIds), false));
            subject.setEndDate(day);
            return;
        }

        int dayCount = futureDays.size();
        int perDay = (int) Math.ceil((double) pendingTopicIds.size() / dayCount);

        int index = 0;
        for (ScheduleDay day : futureDays) {
            int end = Math.min(index + perDay, pendingTopicIds.size());
            if (index >= end) break;
            day.getTopicIds().addAll(pendingTopicIds.subList(index, end));
            index = end;
        }
    }

    // ---------- shared helpers ----------

    private List<String> collectOverduePendingTopicIds(Subject subject, LocalDate today) {
        Set<String> topicIds = new LinkedHashSet<>();
        for (ScheduleDay day : subject.getSchedule()) {
            if (day.getDate().isBefore(today)) {
                topicIds.addAll(day.getTopicIds());
            }
        }
        Set<String> pendingIds = subject.getTopics().stream()
                .filter(t -> "PENDING".equals(t.getStatus()))
                .map(Topic::getTopicId)
                .collect(Collectors.toSet());

        topicIds.retainAll(pendingIds);
        return new ArrayList<>(topicIds);
    }

    private void markRescheduled(Subject subject, List<String> topicIds) {
        Set<String> idSet = new HashSet<>(topicIds);
        subject.getTopics().stream()
                .filter(t -> idSet.contains(t.getTopicId()))
                .forEach(t -> t.setStatus("RESCHEDULED"));
    }

    private void removeFromOverdueDays(Subject subject, LocalDate today, List<String> topicIds) {
        Set<String> idSet = new HashSet<>(topicIds);
        for (ScheduleDay day : subject.getSchedule()) {
            if (day.getDate().isBefore(today)) {
                day.getTopicIds().removeAll(idSet);
            }
        }
    }
}
