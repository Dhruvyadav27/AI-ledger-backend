package com.studyplan.ai.service;

import com.studyplan.ai.dto.TodaySubjectDto;
import com.studyplan.ai.dto.TopicDto;
import com.studyplan.ai.model.ScheduleDay;
import com.studyplan.ai.model.Subject;
import com.studyplan.ai.model.Topic;
import com.studyplan.ai.repository.SubjectRepository;
import com.studyplan.ai.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Powers GET /api/dashboard/today - across ALL of the user's active
 * subjects, find whichever ScheduleDay has date == today, and return
 * just that day's topics. A subject with nothing scheduled for today
 * (already finished, or day is in the future) is simply left out.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SubjectRepository subjectRepository;

    public List<TodaySubjectDto> getTodayTopics() {
        LocalDate today = LocalDate.now();
        String userId = CurrentUser.id();

        return subjectRepository.findByUserId(userId).stream()
                .filter(s -> "ACTIVE".equals(s.getStatus()))
                .map(subject -> toTodayDto(subject, today))
                .filter(dto -> dto != null && !dto.getTopics().isEmpty())
                .toList();
    }

    private TodaySubjectDto toTodayDto(Subject subject, LocalDate today) {
        ScheduleDay todayEntry = subject.getSchedule().stream()
                .filter(d -> today.equals(d.getDate()))
                .findFirst()
                .orElse(null);

        if (todayEntry == null) return null;

        Set<String> todayTopicIds = Set.copyOf(todayEntry.getTopicIds());

        List<TopicDto> topics = subject.getTopics().stream()
                .filter(t -> todayTopicIds.contains(t.getTopicId()))
                .map(this::toTopicDto)
                .toList();

        return TodaySubjectDto.builder()
                .subjectId(subject.getId())
                .subjectTitle(subject.getTitle())
                .dayNumber(todayEntry.getDayNumber())
                .topics(topics)
                .build();
    }

    private TopicDto toTopicDto(Topic t) {
        return TopicDto.builder()
                .topicId(t.getTopicId())
                .title(t.getTitle())
                .orderIndex(t.getOrderIndex())
                .weight(t.getWeight())
                .status(t.getStatus())
                .build();
    }
}