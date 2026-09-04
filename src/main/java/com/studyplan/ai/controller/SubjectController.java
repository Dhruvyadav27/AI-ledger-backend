package com.studyplan.ai.controller;

import com.studyplan.ai.dto.CreateSubjectRequest;
import com.studyplan.ai.dto.SubjectResponse;
import com.studyplan.ai.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.studyplan.ai.dto.AddTopicRequest;
import com.studyplan.ai.dto.ReorderTopicsRequest;
import com.studyplan.ai.dto.UpdateTopicTitleRequest;

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
    @PatchMapping("/{id}/topics/{topicId}")
    public ResponseEntity<SubjectResponse> renameTopic(
            @PathVariable String id, @PathVariable String topicId,
            @Valid @RequestBody UpdateTopicTitleRequest request
    ) {
        return ResponseEntity.ok(subjectService.renameTopic(id, topicId, request.getTitle()));
    }

    @DeleteMapping("/{id}/topics/{topicId}")
    public ResponseEntity<SubjectResponse> deleteTopic(@PathVariable String id, @PathVariable String topicId) {
        return ResponseEntity.ok(subjectService.deleteTopic(id, topicId));
    }

    @PostMapping("/{id}/topics")
    public ResponseEntity<SubjectResponse> addTopic(
            @PathVariable String id, @Valid @RequestBody AddTopicRequest request
    ) {
        return ResponseEntity.ok(subjectService.addTopic(id, request.getTitle()));
    }

    @PatchMapping("/{id}/topics/reorder")
    public ResponseEntity<SubjectResponse> reorderTopics(
            @PathVariable String id, @Valid @RequestBody ReorderTopicsRequest request
    ) {
        return ResponseEntity.ok(subjectService.reorderTopics(id, request.getTopicIds()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubject(@PathVariable String id) {
        subjectService.deleteSubject(id);
        return ResponseEntity.noContent().build();
    }
}