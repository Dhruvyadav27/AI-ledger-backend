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

    public GeminiService(
            WebClient geminiWebClient,
            ObjectMapper objectMapper,
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.api.model}") String model
    ) {
        this.geminiWebClient = geminiWebClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    public List<GeminiTopicResult> orderAndWeighTopics(List<String> rawTopics) {
        try {
            String prompt = buildPrompt(rawTopics);
            String rawResponseText = callGemini(prompt);
            List<GeminiTopicResult> parsed = parseResponse(rawResponseText);

            if (parsed.isEmpty() || parsed.size() != rawTopics.size()) {
                log.warn("Gemini returned {} items for {} topics - using fallback",
                        parsed.size(), rawTopics.size());
                return fallback(rawTopics);
            }
            return parsed;

        } catch (Exception e) {
            log.error("Gemini call failed, using fallback ordering: {}", e.getMessage());
            return fallback(rawTopics);
        }
    }

    private String buildPrompt(List<String> rawTopics) {
        String topicList = String.join("\n", rawTopics.stream().map(t -> "- " + t).toList());
        return """
                You are ordering study topics by prerequisite sequence and rating difficulty.

                Given this raw list of topics for a student to study:
                %s

                Return ONLY a JSON array (no markdown, no explanation) with exactly one
                object per topic, in this exact shape:
                [{"title": "<topic title, exactly as given>", "order": <int starting at 1>, "weight": <int 1-5>}]

                Rules:
                - "order" reflects logical prerequisite sequencing (fundamentals first).
                - "weight" is a 1-5 difficulty/time estimate (1 = quick/easy, 5 = hard/long).
                - Include every topic from the input list exactly once.
                - Output must be valid JSON and nothing else.
                """.formatted(topicList);
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
