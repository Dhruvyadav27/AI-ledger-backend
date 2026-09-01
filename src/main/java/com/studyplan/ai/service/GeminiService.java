package com.studyplan.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyplan.ai.dto.GeminiTopicResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Talks to the Gemini API to order + weight a raw topic list.
 *
 * Gemini endpoint shape:
 *   POST {base-url}/models/{model}:generateContent?key={apiKey}
 *   body: { "contents": [{ "parts": [{ "text": <prompt> }] }],
 *           "generationConfig": { "responseMimeType": "application/json" } }
 *   response: candidates[0].content.parts[0].text  <- raw JSON string
 *
 * responseMimeType=application/json forces Gemini to return ONLY valid
 * JSON text (no markdown fences, no chatty preamble) - this is what
 * makes parsing reliable.
 *
 * If ANYTHING goes wrong (network error, malformed JSON, empty list),
 * we fall back to equal weight (3) and original list order - per the
 * spec's "IMPORTANT CONSTRAINTS" requirement. The user's subject
 * creation never fails just because the AI call had a hiccup.
 */
@Slf4j
@Service
public class GeminiService {

    private final WebClient geminiWebClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    private final int maxWeightPerDay;

    public GeminiService(
            WebClient geminiWebClient,
            ObjectMapper objectMapper,
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.api.model}") String model,
            @Value("${study.max-weight-per-day}") int maxWeightPerDay
    ) {
        this.geminiWebClient = geminiWebClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.maxWeightPerDay = maxWeightPerDay;
    }

    public List<GeminiTopicResult> orderAndWeighTopics(
            List<String> rawTopics, String mode, Integer totalDays, Integer dailyTopicCount
    ) {
        try {
            String prompt = buildPrompt(rawTopics, mode, totalDays, dailyTopicCount);
            String rawResponseText = callGemini(prompt);
            List<GeminiTopicResult> parsed = parseResponse(rawResponseText);

            if (parsed.isEmpty()) {
                log.warn("Gemini returned empty result - using fallback");
                return fallback(rawTopics);
            }
            return parsed;

        } catch (Exception e) {
            log.error("Gemini call failed, using fallback ordering: {}", e.getMessage());
            return fallback(rawTopics);
        }
    }

    private String buildPrompt(List<String> rawTopics, String mode, Integer totalDays, Integer dailyTopicCount) {
        String topicList = String.join("\n", rawTopics.stream().map(t -> "- " + t).toList());

        String paceContext = "PACE".equals(mode)
                ? "The student studies " + dailyTopicCount + " topics per day, with no fixed deadline."
                : "The student has exactly " + totalDays + " days total to cover everything.";

        return """
                You are building a granular, day-by-day study curriculum from a raw list
                of topics a student wants to learn. %s

                Raw input topics (these may be broad section/chapter names, not
                individual lessons):
                %s

                YOUR JOB: Treat each input line as a section heading, not a final study
                item. EXPAND every section into the actual concrete, specific lessons a
                student would work through to master it - the way a real course syllabus
                breaks a chapter into individual lessons. Do NOT return a broad heading
                like "REST API Development" as a single item - break it into its real
                sub-lessons (e.g. "Creating REST Controllers", "Request Mapping & Path
                Variables", "Exception Handling in REST APIs", "Building DTOs and
                Validation", etc). A single broad section commonly expands into 10-30+
                granular lessons depending on how much real content it holds - do not
                under-expand.

                Each output item must be small enough to realistically finish in ONE
                focused study day or less.

                Return ONLY a JSON array (no markdown, no explanation), one object per
                granular lesson, in this exact shape:
                [{"title": "<specific, concrete lesson title>", "order": <int>, "weight": <int 1-3>}]

                Rules:
                - "order" is ONE continuous sequence (1, 2, 3...) across the entire
                  output, in the correct logical/prerequisite study order.
                - "weight" must be between 1 and 3 for every item (1 = quick, 3 = a
                  solid day's work). Never output a weight above 3 - if something feels
                  bigger than a 3, that means it needs to be split further.
                - Titles must be specific and descriptive, never generic ("Part 1",
                  "Basics", "Advanced Topics 2").
                - Cover the full depth of every input topic - don't skip content to
                  keep the list short.
                - Output must be valid JSON and nothing else.
                """.formatted(paceContext, topicList);
    }

    private String callGemini(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of("responseMimeType", "application/json")
        );

        JsonNode response = geminiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/models/{model}:generateContent")
                        .queryParam("key", apiKey)
                        .build(model))
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        return response
                .path("candidates").path(0)
                .path("content").path("parts").path(0)
                .path("text").asText();
    }

    private List<GeminiTopicResult> parseResponse(String rawJson) throws Exception {
        GeminiTopicResult[] results = objectMapper.readValue(rawJson, GeminiTopicResult[].class);
        return List.of(results);
    }

    /** Equal weight (3), original input order preserved - the documented fallback. */
    private List<GeminiTopicResult> fallback(List<String> rawTopics) {
        List<GeminiTopicResult> fallbackList = new ArrayList<>();
        for (int i = 0; i < rawTopics.size(); i++) {
            fallbackList.add(new GeminiTopicResult(rawTopics.get(i), i + 1, 3));
        }
        return fallbackList;
    }
}
