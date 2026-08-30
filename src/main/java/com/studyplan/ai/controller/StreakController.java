package com.studyplan.ai.controller;

import com.studyplan.ai.dto.StreakDto;
import com.studyplan.ai.model.User;
import com.studyplan.ai.repository.UserRepository;
import com.studyplan.ai.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StreakController {

    private final UserRepository userRepository;

    @GetMapping("/api/streak")
    public StreakDto streak() {
        User user = userRepository.findById(CurrentUser.id()).orElseThrow();
        return StreakDto.builder()
                .currentStreak(user.getStreak().getCurrentStreak())
                .longestStreak(user.getStreak().getLongestStreak())
                .lastActiveDate(user.getStreak().getLastActiveDate())
                .build();
    }
}