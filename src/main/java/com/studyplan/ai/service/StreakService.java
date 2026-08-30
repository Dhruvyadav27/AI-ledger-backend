package com.studyplan.ai.service;

import com.studyplan.ai.model.Streak;
import com.studyplan.ai.model.User;
import com.studyplan.ai.repository.UserRepository;
import com.studyplan.ai.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Pure streak math, kept separate from ProgressLogService so the "how do
 * dates turn into a streak count" logic is easy to find and test.
 *
 * Rule: currentStreak counts consecutive CALENDAR days with at least one
 * completed topic. Completing a 2nd, 3rd... topic on the SAME day never
 * increases the streak further - only the first completion of a new day
 * does.
 */
@Service
@RequiredArgsConstructor
public class StreakService {

    private final UserRepository userRepository;

    /** Call this when a topic transitions PENDING -> DONE. */
    public void onTopicCompleted(LocalDate completionDate) {
        User user = userRepository.findById(CurrentUser.id()).orElseThrow();
        Streak streak = user.getStreak();
        LocalDate lastActive = streak.getLastActiveDate();

        if (lastActive != null && lastActive.equals(completionDate)) {
            // Already counted today - a 2nd+ completion same day, no-op.
            return;
        }

        if (lastActive != null && lastActive.equals(completionDate.minusDays(1))) {
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
        } else {
            // First ever completion, or there was a gap - streak restarts.
            streak.setCurrentStreak(1);
        }

        streak.setLongestStreak(Math.max(streak.getLongestStreak(), streak.getCurrentStreak()));
        streak.setLastActiveDate(completionDate);

        userRepository.save(user);
    }

    /**
     * Call this when a topic is un-toggled (DONE -> PENDING) and it was
     * the LAST completed topic for that date (progress log count hit 0).
     * Conservative rule: only roll back if the undone date IS today's
     * lastActiveDate - avoids messing with streak history for older days.
     */
    public void onCompletionUndoneAndDayNowEmpty(LocalDate date) {
        User user = userRepository.findById(CurrentUser.id()).orElseThrow();
        Streak streak = user.getStreak();

        if (date.equals(streak.getLastActiveDate()) && streak.getCurrentStreak() > 0) {
            streak.setCurrentStreak(streak.getCurrentStreak() - 1);
            streak.setLastActiveDate(date.minusDays(1));
            userRepository.save(user);
        }
    }
}
