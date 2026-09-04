package com.studyplan.ai.service;

import com.studyplan.ai.dto.*;
import com.studyplan.ai.exception.ApiException;
import com.studyplan.ai.model.ScheduleDay;
import com.studyplan.ai.model.Subject;
import com.studyplan.ai.model.Topic;
import com.studyplan.ai.repository.SubjectRepository;
import com.studyplan.ai.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.UUID;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Orchestrates subject creation end to end:
 *   1. Validate mode-specific fields (totalDays for DEADLINE, dailyTopicCount for PACE)
 *   2. Send rawTopics to Gemini -> get back ordered + weighted results
 *   3. Build Topic objects (with generated topicIds), sorted by order
 *   4. Build the day-wise schedule (ScheduleBuilderService)
 *   5. Save the Subject, scoped to the logged-in user
 *   6. Map to SubjectResponse DTO for the controller to return
 */
@Service
@RequiredArgsConstructor
public class SubjectService {

    private final GeminiService geminiService;
    private final ScheduleBuilderService scheduleBuilderService;
    private final SubjectRepository subjectRepository;
    private final ProgressLogService progressLogService;

    public SubjectResponse createSubject(CreateSubjectRequest request) {
        validateModeFields(request);

        List<GeminiTopicResult> aiResults = geminiService.orderAndWeighTopics(
                request.getRawTopics(), request.getMode(), request.getTotalDays(), request.getDailyTopicCount());

        List<Topic> topics = toSortedTopics(aiResults);

        LocalDate startDate = LocalDate.now();
        List<ScheduleDay> schedule = scheduleBuilderService.buildSchedule(
                topics,
                request.getMode(),
                request.getTotalDays(),
                request.getDailyTopicCount(),
                startDate
        );

        LocalDate endDate = schedule.isEmpty()
                ? startDate
                : schedule.get(schedule.size() - 1).getDate();

        Subject subject = Subject.builder()
                .userId(CurrentUser.id())
                .title(request.getTitle())
                .mode(request.getMode())
                .totalDays(request.getTotalDays())
                .dailyTopicCount(request.getDailyTopicCount())
                .rescheduleStrategy(request.getRescheduleStrategy())
                .startDate(startDate)
                .endDate(endDate)
                .status("ACTIVE")
                .topics(topics)
                .schedule(schedule)
                .build();

        Subject saved = subjectRepository.save(subject);
        return toResponse(saved);
    }

    public List<SubjectResponse> getSubjectsForCurrentUser() {
        return subjectRepository.findByUserId(CurrentUser.id())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public SubjectResponse getSubjectById(String id) {
        Subject subject = subjectRepository.findByIdAndUserId(id, CurrentUser.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subject not found"));
        return toResponse(subject);
    }
    public void toggleTopicCompletion(String subjectId, String topicId) {
        Subject subject = subjectRepository.findByIdAndUserId(subjectId, CurrentUser.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subject not found"));

        Topic topic = subject.getTopics().stream()
                .filter(t -> t.getTopicId().equals(topicId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Topic not found"));

        boolean wasDone = "DONE".equals(topic.getStatus());
        topic.setStatus(wasDone ? "PENDING" : "DONE");

        // Recompute completed flag for every schedule day this topic appears in
        for (ScheduleDay day : subject.getSchedule()) {
            if (day.getTopicIds().contains(topicId)) {
                boolean allDone = day.getTopicIds().stream()
                        .allMatch(id -> subject.getTopics().stream()
                                .anyMatch(t -> t.getTopicId().equals(id) && "DONE".equals(t.getStatus())));
                day.setCompleted(allDone);
            }
        }

        subjectRepository.save(subject);

        // Log + streak hooks - fire AFTER the subject is saved
        if (wasDone) {
            progressLogService.recordUndo(subjectId);
        } else {
            progressLogService.recordCompletion(subjectId);
        }
    }
    public SubjectResponse renameTopic(String subjectId, String topicId, String newTitle) {
        Subject subject = subjectRepository.findByIdAndUserId(subjectId, CurrentUser.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subject not found"));

        Topic topic = subject.getTopics().stream()
                .filter(t -> t.getTopicId().equals(topicId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Topic not found"));

        topic.setTitle(newTitle);
        // Renaming doesn't change order/weight, so the schedule stays valid as-is.
        subjectRepository.save(subject);
        return toResponse(subject);
    }

    public SubjectResponse deleteTopic(String subjectId, String topicId) {
        Subject subject = subjectRepository.findByIdAndUserId(subjectId, CurrentUser.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subject not found"));

        boolean removed = subject.getTopics().removeIf(t -> t.getTopicId().equals(topicId));
        if (!removed) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Topic not found");
        }

        rebuildSchedule(subject);
        subjectRepository.save(subject);
        return toResponse(subject);
    }

    public SubjectResponse addTopic(String subjectId, String title) {
        Subject subject = subjectRepository.findByIdAndUserId(subjectId, CurrentUser.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subject not found"));

        int nextOrder = subject.getTopics().stream()
                .mapToInt(Topic::getOrderIndex).max().orElse(0) + 1;

        Topic newTopic = new Topic(
                UUID.randomUUID().toString().substring(0, 8),
                title,
                nextOrder,
                1, // manually added topics default to the lightest weight
                "PENDING"
        );
        subject.getTopics().add(newTopic);

        rebuildSchedule(subject);
        subjectRepository.save(subject);
        return toResponse(subject);
    }

    public SubjectResponse reorderTopics(String subjectId, List<String> orderedTopicIds) {
        Subject subject = subjectRepository.findByIdAndUserId(subjectId, CurrentUser.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subject not found"));

        Map<String, Integer> newOrderMap = new HashMap<>();
        for (int i = 0; i < orderedTopicIds.size(); i++) {
            newOrderMap.put(orderedTopicIds.get(i), i + 1);
        }

        for (Topic topic : subject.getTopics()) {
            Integer newOrder = newOrderMap.get(topic.getTopicId());
            if (newOrder != null) {
                topic.setOrderIndex(newOrder);
            }
        }
        subject.setTopics(subject.getTopics().stream()
                .sorted(Comparator.comparingInt(Topic::getOrderIndex))
                .collect(Collectors.toList()));


        rebuildSchedule(subject);
        subjectRepository.save(subject);
        return toResponse(subject);
    }

    public void deleteSubject(String subjectId) {
        Subject subject = subjectRepository.findByIdAndUserId(subjectId, CurrentUser.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subject not found"));
        subjectRepository.delete(subject);
    }

    /**
     * Re-runs the scheduling algorithm from scratch using the subject's
     * CURRENT topic list (post add/delete/reorder), keeping the original
     * startDate. Already-DONE topics keep their status - they just get
     * placed into the freshly rebuilt day slots like everything else.
     */
    private void rebuildSchedule(Subject subject) {
        List<Topic> sortedTopics = subject.getTopics().stream()
                .sorted(Comparator.comparingInt(Topic::getOrderIndex))
                .toList();

        List<ScheduleDay> newSchedule = scheduleBuilderService.buildSchedule(
                sortedTopics,
                subject.getMode(),
                subject.getTotalDays(),
                subject.getDailyTopicCount(),
                subject.getStartDate()
        );

        subject.setSchedule(newSchedule);

        LocalDate newEndDate = newSchedule.isEmpty()
                ? subject.getStartDate()
                : newSchedule.get(newSchedule.size() - 1).getDate();
        subject.setEndDate(newEndDate);
    }

    // ---------- helpers ----------

    private void validateModeFields(CreateSubjectRequest request) {
        if ("DEADLINE".equals(request.getMode())) {
            if (request.getTotalDays() == null || request.getTotalDays() < 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "totalDays is required for DEADLINE mode");
            }
        } else if ("PACE".equals(request.getMode())) {
            if (request.getDailyTopicCount() == null || request.getDailyTopicCount() < 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "dailyTopicCount is required for PACE mode");
            }
        }
        // REDISTRIBUTE strategy only makes sense with a fixed deadline
        if ("REDISTRIBUTE".equals(request.getRescheduleStrategy()) && "PACE".equals(request.getMode())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "REDISTRIBUTE strategy is only valid for DEADLINE mode");
        }
    }

    private List<Topic> toSortedTopics(List<GeminiTopicResult> aiResults) {
        List<Topic> topics = new ArrayList<>();
        for (GeminiTopicResult r : aiResults) {
            topics.add(new Topic(
                    UUID.randomUUID().toString().substring(0, 8), // short unique topicId
                    r.getTitle(),
                    r.getOrder(),
                    r.getWeight(),
                    "PENDING"
            ));
        }
        topics.sort(Comparator.comparingInt(Topic::getOrderIndex));
        return topics;
    }

    private SubjectResponse toResponse(Subject subject) {
        List<TopicDto> topicDtos = subject.getTopics().stream()
                .map(t -> TopicDto.builder()
                        .topicId(t.getTopicId())
                        .title(t.getTitle())
                        .orderIndex(t.getOrderIndex())
                        .weight(t.getWeight())
                        .status(t.getStatus())
                        .build())
                .toList();

        List<ScheduleDayDto> scheduleDtos = subject.getSchedule().stream()
                .map(d -> ScheduleDayDto.builder()
                        .dayNumber(d.getDayNumber())
                        .date(d.getDate())
                        .topicIds(d.getTopicIds())
                        .completed(d.isCompleted())
                        .build())
                .toList();

        return SubjectResponse.builder()
                .id(subject.getId())
                .title(subject.getTitle())
                .mode(subject.getMode())
                .totalDays(subject.getTotalDays())
                .dailyTopicCount(subject.getDailyTopicCount())
                .rescheduleStrategy(subject.getRescheduleStrategy())
                .startDate(subject.getStartDate())
                .endDate(subject.getEndDate())
                .status(subject.getStatus())
                .topics(topicDtos)
                .schedule(scheduleDtos)
                .build();
    }
}