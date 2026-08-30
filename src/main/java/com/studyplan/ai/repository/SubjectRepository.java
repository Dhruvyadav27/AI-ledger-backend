package com.studyplan.ai.repository;

import com.studyplan.ai.model.Subject;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends MongoRepository<Subject, String> {

    List<Subject> findByUserId(String userId);

    // Used by SubjectService to verify a subject belongs to the
    // logged-in user before returning/modifying it (data isolation).
    Optional<Subject> findByIdAndUserId(String id, String userId);
}
