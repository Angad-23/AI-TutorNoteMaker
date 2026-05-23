package com.example.myservice.service;

import com.example.myservice.dto.SessionRequest;
import com.example.myservice.entity.SessionNote;
import com.example.myservice.repository.SessionNoteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class OpenAiService {

    @Value("${openai.api.key}")
    private String API_KEY;

    @Value("${openai.api.url}")
    private String API_URL;

    // ✅ Real DB injected here
    private final SessionNoteRepository noteRepository;

    public OpenAiService(SessionNoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    // ✅ Pulls best matching example from YOUR real MySQL data
    private String getBestHistoricalExample(SessionRequest request) {
        String subjectKey = request.getSubject().toLowerCase();
        List<SessionNote> allNotes = noteRepository.findAll();

        // Try to find subject-matching approved note from DB
        Optional<SessionNote> match = allNotes.stream()
                .filter(n -> n.getSubject() != null &&
                        n.getSubject().toLowerCase().contains(subjectKey) &&
                        n.getFinalApprovedNote() != null)
                .findFirst();

        // Fallback: use latest saved note if no subject match
        SessionNote best = match.orElse(
                allNotes.isEmpty() ? null : allNotes.get(allNotes.size() - 1)
        );

        if (best != null) {
            String example = String.format(
                    "Input: Topic: %s. Grade: %s. Engagement: %s. Pointers: %s\nOutput: '%s'",
                    best.getSubject(), best.getGradeLevel(),
                    best.getEngagement(), best.getRawPointers(),
                    best.getFinalApprovedNote()
            );
            // Trim to 300 chars so long old notes don't waste tokens
            return example.length() > 300 ? example.substring(0, 300) + "..." : example;
        }

        // Hardcoded fallback only when DB is completely empty
        return "Input: Topic: Math. Engagement: High.\n" +
                "Output: 'During today's math session, the student demonstrated " +
                "strong understanding and high engagement throughout the session.'";
    }

    public String generateSessionNote(SessionRequest request) {
        RestTemplate restTemplate = new RestTemplate();

        // ✅ Pulls from real MySQL DB dynamically
        String bestHistoricalMatch = getBestHistoricalExample(request);

        String promptBody = String.format(
                "You are an expert academic note writer for Trivium Education Services.\n" +
                        "This session is for a student in %s. Reference that state's curriculum standards briefly.\n\n" +
                        "### EXAMPLE FROM TRIVIUM'S HISTORICAL DATA:\n%s\n" +
                        "### CURRENT SESSION:\n" +
                        "- Student: %s\n- Grade: %s\n- Subject: %s\n" +
                        "- Engagement: %s\n- Key Pointers: %s\n- Homework/Next Steps: %s\n\n" +
                        "### STRICT WRITING RULES:\n" +
                        "1. Maximum 120 words total — be concise and direct\n" +
                        "2. Write ONE short paragraph (3-4 sentences) as the session summary\n" +
                        "3. Add ONE line for curriculum standard reference only\n" +
                        "4. Add a brief 'Next Steps' bullet list with max 2 points\n" +
                        "5. NO long explanations, NO repetition, NO filler sentences\n" +
                        "6. Use student's name naturally but only twice\n\n" +
                        "Write the note now:",
                request.getDistrictOrState(), bestHistoricalMatch,
                request.getStudentName(), request.getGradeLevel(),
                request.getSubject(), request.getEngagement(),
                request.getKeyPointers(), request.getNextSteps()
        );

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "llama-3.1-8b-instant");
        requestBody.put("max_tokens", 400); // ✅ Required by Groq
        requestBody.put("temperature", 0.4);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", promptBody));
        requestBody.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(API_KEY);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(API_URL, entity, Map.class);
            Map<?, ?> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("choices")) {
                List<?> choices = (List<?>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
                    Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
                    if (message != null && message.containsKey("content")) {
                        return (String) message.get("content");
                    }
                }
            }
            return "Error: Unexpected response format from AI server.";

        } catch (Exception e) {
            return "Error generating note: " + e.getMessage();
        }
    }
}