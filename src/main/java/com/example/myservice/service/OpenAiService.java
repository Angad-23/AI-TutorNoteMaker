package com.example.myservice.service;

import com.example.myservice.dto.SessionRequest;
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

    public String generateSessionNote(SessionRequest request) {
        System.out.println("=== DATA RECEIVED FROM UI ===");
        System.out.println("Student:  " + request.getStudentName());
        System.out.println("Subject:  " + request.getSubject());
        System.out.println("State:    " + request.getDistrictOrState());
        System.out.println("Grade:    " + request.getGradeLevel());
        System.out.println("Engage:   " + request.getEngagement());
        System.out.println("Pointers: " + request.getKeyPointers());
        System.out.println("Next:     " + request.getNextSteps());
        System.out.println("=============================");

        RestTemplate restTemplate = new RestTemplate();

        String targetSubject = request.getSubject() != null
                ? request.getSubject().trim().toLowerCase() : "";

        String promptBody = (targetSubject.contains("overall") || targetSubject.contains("group"))
                ? buildGroupSessionPrompt(request)
                : buildSingleStudentPrompt(request);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "llama-3.1-8b-instant");
        requestBody.put("max_tokens", 300);
        requestBody.put("temperature", 0.3);

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

    private String buildSingleStudentPrompt(SessionRequest request) {
        String student    = request.getStudentName()     != null ? request.getStudentName()     : "";
        String engagement = request.getEngagement()      != null ? request.getEngagement()      : "Good";
        String pointers   = request.getKeyPointers()     != null ? request.getKeyPointers()     : "";
        String next       = request.getNextSteps()       != null ? request.getNextSteps()       : "";
        String standard   = request.getCurriculumStandard() != null ? request.getCurriculumStandard() : "";

        // Build standard description cleanly
        // Strip the code part e.g. "[Standard MA.4.FR.1.1: Model and express...]"
        // Keep only the description after the colon
        String standardDesc = "";
        if (!standard.isEmpty() && standard.contains(":")) {
            standardDesc = standard.substring(standard.indexOf(":") + 1)
                    .replace("]", "")
                    .trim();
        }

        return "You are a professional math tutor writing a session note for school officials.\n\n" +

                "VOICE & TONE RULES:\n" +
                "- First-person, past tense, casual tutor-to-tutor voice\n" +
                "- Exactly ONE paragraph, no headers, no bullets, no markdown, no bold\n" +
                "- Sound warm, honest, and specific\n" +
                "- Do NOT start with 'I' — start with the student's name\n\n" +

                "TONE EXAMPLES (VOICE ONLY — do not copy any math content or names):\n" +
                "- \"[Student] was really engaged today and picked up the concept quickly. " +
                "They struggled a bit at first but by the end had a solid grasp of it.\"\n" +
                "- \"[Student] had a tough session today. They were distracted but still managed " +
                "to get through the material. I'll keep working with them on this.\"\n\n" +

                "YOUR TASK — Write ONE paragraph using ONLY these facts:\n\n" +
                "Student Name: " + student + "\n" +
                "Engagement Level: " + engagement + "\n" +
                // ✅ Standard description used to anchor math topic
                "Math Topic / Standard Focus: " + standardDesc + "\n" +
                "Key Observations / Standard Covered: " + pointers + "\n" +
                "Next Steps: " + next + "\n\n" +

                "HARD RULES:\n" +
                "1. ONLY mention math topics explicitly stated in Key Observations above\n" +
                "2. NEVER mention rounding, area model, or ANY topic unless in Key Observations\n" +
                "3. NEVER use names other than: " + student + "\n" +
                "4. Do NOT copy or paraphrase the tone examples above\n" +
                "5. Write exactly one paragraph with no line breaks\n" +
                "6. Write like talking to a colleague — use contractions and show personality\n" +
                "7. Minimum 3-4 sentences with specific details\n" +
                "8. NEVER quote standard codes (like 'MA.4.FR.1.1') — use plain language\n" +
                "9. Math concept MUST come from Key Observations only — never infer from standard code\n\n" +
                "Write the note now:";
    }

    private String buildGroupSessionPrompt(SessionRequest request) {
        String students   = request.getStudentName()     != null ? request.getStudentName()     : "";
        String engagement = request.getEngagement()      != null ? request.getEngagement()      : "Good";
        String pointers   = request.getKeyPointers()     != null ? request.getKeyPointers()     : "";
        String next       = request.getNextSteps()       != null ? request.getNextSteps()       : "";
        String standard   = request.getCurriculumStandard() != null ? request.getCurriculumStandard() : "";

        // Build standard description cleanly
        // Strip the code part e.g. "[Standard MA.4.FR.1.1: Model and express...]"
        // Keep only the description after the colon
        String standardDesc = "";
        if (!standard.isEmpty() && standard.contains(":")) {
            standardDesc = standard.substring(standard.indexOf(":") + 1)
                    .replace("]", "")
                    .trim();
        }

        return "You are a math tutor writing a group session note for school officials.\n\n" +

                "VOICE & TONE RULES:\n" +
                "- First-person, past tense, casual tutor-to-tutor voice\n" +
                "- Exactly ONE paragraph, no headers, no bullets, no markdown\n" +
                "- Mention specific games and tools BY NAME if listed in observations\n" +
                "- Be honest about individual struggles and group wins\n\n" +

                "TONE EXAMPLE (VOICE ONLY — do not copy content):\n" +
                "- \"Today we started with a quick icebreaker, then moved into the main activity. " +
                "The students were engaged overall, though one struggled more than the others. " +
                "We finished with a Blooket and I'll pick up where we left off next session.\"\n\n" +

                "YOUR TASK — Write ONE paragraph using ONLY these facts:\n\n" +
                "Student Names: " + students + "\n" +
                "Engagement Level: " + engagement + "\n" +
                // ✅ Standard description used to anchor math topic
                "Math Topic / Standard Focus: " + standardDesc + "\n" +
                "Key Observations (activities, games, struggles): " + pointers + "\n" +
                "Next Steps: " + next + "\n\n" +

                "HARD RULES:\n" +
                "1. ONLY mention activities and topics from Key Observations above\n" +
                "2. Name Blooket, IXL, Penguin Run, or any game if mentioned in observations\n" +
                "3. Do NOT invent ANY details not explicitly in Key Observations — " +
                "   this includes math sub-topics, student behaviors, or extra activities\n" + // ✅ stronger
                "4. ONLY use these student names: " + students + "\n" +
                "5. Start the note by naming the students — never say 'everyone' or 'the group'\n" + // ✅ new
                "6. Write exactly one paragraph\n" +
                "7. NEVER quote standard codes — describe concepts in plain language\n" +
                "8. Math concept MUST come from Key Observations only\n" +
                "9. NEVER state engagement as a metric like 'engagement level was high' — " +
                "   weave it naturally into the narrative instead\n" + // ✅ new
                "10. NEVER invent math sub-topics like 'mixed numbers' or 'fractions greater than one' " +
                "    unless explicitly mentioned in Key Observations\n\n"+
                "11. Do NOT copy phrases verbatim from the standard description — " +
                "paraphrase the math concept naturally in tutor language\n"; // ✅ new
    }
}