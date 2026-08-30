package com.studyplan.ai.service;

import com.studyplan.ai.model.ScheduleDay;
import com.studyplan.ai.model.Topic;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns an ORDERED list of topics (already sorted by orderIndex, already
 * weighted by Gemini) into a day-wise schedule. Two completely different
 * algorithms depending on mode:
 *
 * PACE mode: no deadline pressure. Just chunk topics into fixed-size
 * groups of `dailyTopicCount`, one chunk per day, in order. End date
 * falls out naturally from how many days that takes.
 *
 * DEADLINE mode: the opposite problem - a FIXED number of days
 * (`totalDays`) must fit ALL topics. So instead of a fixed topic-count
 * per day, we balance by WEIGHT: harder topics (higher weight) eat up
 * more of a day's "budget", so a day might get 1 heavy topic or 3 light
 * ones. This is what the spec means by "harder topics get more
 * days/slots".
 */
@Service
public class ScheduleBuilderService {

    public List<ScheduleDay> buildSchedule(
            List<Topic> orderedTopics,
            String mode,
            Integer totalDays,
            Integer dailyTopicCount,
            LocalDate startDate
    ) {
        if ("PACE".equals(mode)) {
            return buildPaceSchedule(orderedTopics, dailyTopicCount, startDate);
        }
        return buildDeadlineSchedule(orderedTopics, totalDays, startDate);
    }

    // ---------- PACE MODE ----------
    // Simple: fixed topics-per-day, chunk in order, no deadline to respect.
    private List<ScheduleDay> buildPaceSchedule(
            List<Topic> orderedTopics, int dailyTopicCount, LocalDate startDate
    ) {
        List<ScheduleDay> days = new ArrayList<>();
        List<String> currentChunk = new ArrayList<>();
        int dayNumber = 1;

        for (Topic topic : orderedTopics) {
            currentChunk.add(topic.getTopicId());

            if (currentChunk.size() == dailyTopicCount) {
                days.add(newDay(dayNumber, startDate, currentChunk));
                currentChunk = new ArrayList<>();
                dayNumber++;
            }
        }

        // leftover topics that didn't fill a complete day
        if (!currentChunk.isEmpty()) {
            days.add(newDay(dayNumber, startDate, currentChunk));
        }

        return days;
    }

    // ---------- DEADLINE MODE ----------
    // Weighted cumulative bin-packing: close a day once its cumulative
    // weight crosses the "ideal weight per day" boundary for that day
    // number. Order is never disturbed (topics stay in prerequisite
    // sequence) - we only decide WHERE to cut the sequence into days.
    private List<ScheduleDay> buildDeadlineSchedule(
            List<Topic> orderedTopics, int totalDays, LocalDate startDate
    ) {
        totalDays = Math.max(1, totalDays);

        double totalWeight = orderedTopics.stream().mapToInt(Topic::getWeight).sum();
        double idealWeightPerDay = totalWeight / totalDays;

        List<ScheduleDay> days = new ArrayList<>();
        List<String> currentChunk = new ArrayList<>();

        int dayNumber = 1;
        double runningWeight = 0;
        double dayBoundary = idealWeightPerDay; // cumulative weight target to finish day 1

        for (int i = 0; i < orderedTopics.size(); i++) {
            Topic topic = orderedTopics.get(i);
            currentChunk.add(topic.getTopicId());
            runningWeight += topic.getWeight();

            boolean isLastTopic = (i == orderedTopics.size() - 1);
            boolean crossedBoundary = runningWeight >= dayBoundary;
            boolean daysRemain = dayNumber < totalDays;

            if (crossedBoundary && daysRemain && !isLastTopic) {
                days.add(newDay(dayNumber, startDate, currentChunk));
                currentChunk = new ArrayList<>();
                dayNumber++;
                dayBoundary = idealWeightPerDay * dayNumber; // next cumulative target
            }
        }

        // whatever's left (including everything, if totalWeight was 0
        // or all topics landed in the last bucket) becomes the final day
        if (!currentChunk.isEmpty()) {
            days.add(newDay(dayNumber, startDate, currentChunk));
        }

        return days;
    }

    private ScheduleDay newDay(int dayNumber, LocalDate startDate, List<String> topicIds) {
        return new ScheduleDay(
                dayNumber,
                startDate.plusDays(dayNumber - 1L),
                new ArrayList<>(topicIds),
                false
        );
    }
}