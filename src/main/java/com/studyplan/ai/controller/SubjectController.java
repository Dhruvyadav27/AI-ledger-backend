package com.studyplan.ai.controller;

import com.studyplan.ai.dto.CreateSubjectRequest;
import com.studyplan.ai.dto.SubjectResponse;
import com.studyplan.ai.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * All endpoints here require a valid JWT (enforced globally in
 * SecurityConfig - only /api/auth/** is public).
 */
@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @PostMapping
    public ResponseEntity<SubjectResponse> create(@Valid @RequestBody CreateSubjectRequest request) {
        SubjectResponse response = subjectService.createSubject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SubjectResponse>> getAll() {
        return ResponseEntity.ok(subjectService.getSubjectsForCurrentUser());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubjectResponse> getOne(@PathVariable String id) {
        return ResponseEntity.ok(subjectService.getSubjectById(id));
    }
    @PatchMapping("/{id}/topics/{topicId}/complete")
    public ResponseEntity<Void> completeTopic(@PathVariable String id, @PathVariable String topicId) {
        subjectService.toggleTopicCompletion(id, topicId);
        return ResponseEntity.noContent().build();
    }
}