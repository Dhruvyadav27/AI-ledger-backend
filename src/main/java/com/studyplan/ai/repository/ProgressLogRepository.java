package com.studyplan.ai.repository;

import com.studyplan.ai.model.ProgressLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProgressLogRepository extends MongoRepository<ProgressLog, String> {
    Optional<ProgressLog> findByUserIdAndDate(String userId, LocalDate date);
    List<ProgressLog> findByUserIdAndDateBetween(String userId, LocalDate start, LocalDate end);
}
