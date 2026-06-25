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
        RestTemplate restTemplate = new RestTemplate();

        String targetSubject = request.getSubject() != null
                ? request.getSubject().trim().toLowerCase() : "";

        String promptBody = (targetSubject.contains("overall") || targetSubject.contains("group"))
                ? buildGroupSessionPrompt(request)
                : buildSingleStudentPrompt(request);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "llama-3.1-8b-instant");
        requestBody.put("max_tokens", 400);
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
        String student        = request.getStudentName()        != null ? request.getStudentName()        : "";
        String engagementLvl  = request.getEngagement()         != null ? request.getEngagement()         : "Good";
        String engagementNotes= request.getEngagementNotes()    != null ? request.getEngagementNotes()    : "";
        String skillsNotes    = request.getSkillsNotes()        != null ? request.getSkillsNotes()        : "";
        String next           = request.getNextSteps()          != null ? request.getNextSteps()          : "";
        String standard       = request.getCurriculumStandard() != null ? request.getCurriculumStandard() : "";

        // Extract clean description from standard code
        String standardDesc = "";
        if (!standard.isEmpty() && standard.contains(":")) {
            standardDesc = standard.substring(standard.indexOf(":") + 1)
                    .replace("]", "").trim();
        }

        return "You are a professional math tutor writing a session note for school officials.\n\n" +

                "VOICE & TONE RULES:\n" +
                "- First-person, past tense, casual tutor-to-tutor voice\n" +
                "- Exactly ONE paragraph, no headers, no bullets, no markdown, no bold\n" +
                "- Sound warm, honest, and specific\n" +
                "- Do NOT start with 'I' — start with the student's name\n\n" +

                "TONE EXAMPLES (VOICE ONLY — do not copy any math content or names):\n" +
                "- \"[Student] was really engaged today and picked up the concept quickly. " +
                "He/she struggled a bit at first but by the end had a solid grasp of it.\"\n" +
                "- \"[Student] had a tough session today. He/she was distracted but still managed " +
                "to get through the material. I'll keep working with him/her on this.\"\n\n" +

                "YOUR TASK — Write ONE paragraph weaving BOTH the behaviour and skills details naturally together:\n\n" +
                "Student Name: " + student + "\n" +
                "Student Pronouns: " + request.getPronouns() + "\n" +
                "Engagement Level: " + engagementLvl + "\n" +
                "Math Topic / Standard Focus: " + standardDesc + "\n" +
                "Engagement & Behaviour: " + engagementNotes + "\n" +
                "Skills & Specific Moments: " + skillsNotes + "\n" +
                "Next Steps: " + next + "\n\n" +

                "HARD RULES:\n" +
                "1. Naturally blend the behaviour observations with the skills/math observations in one flowing paragraph\n" +
                "2. Math topic MUST come from 'Math Topic / Standard Focus' above\n" +
                "3. Specific details MUST come from 'Engagement & Behaviour' and 'Skills & Specific Moments'\n" +
                "4. NEVER invent details not in the data above\n" +
                "5. NEVER use names other than: " + student + "\n" +
                "6. Write exactly one paragraph, no line breaks within it\n" +
                "7. Write like talking to a colleague — contractions, warmth, personality\n" +
                "8. Minimum 3-4 sentences with specific details\n" +
                "9. NEVER mention grade level or standard codes\n" +
                "10. NEVER state engagement as a metric — weave it naturally into the narrative\n" +
                "11. Always use " + request.getPronouns() + " pronouns for " + student +
                " — NEVER use they/them unless the pronouns field above says 'they/them'\n\n" +
                "12. Reference the math topic conversationally — never explain the standard " +
                "or quote its code\n" +
                "13. Describe ONLY what is in the observations — never invent problems, " +
                "concepts, or mistakes not recorded\n\n" +
                "Write the note now:";
    }

    private String buildGroupSessionPrompt(SessionRequest request) {
        String students       = request.getStudentName()        != null ? request.getStudentName()        : "Group";
        String engagementLvl  = request.getEngagement()         != null ? request.getEngagement()         : "Good";
        String engagementNotes= request.getEngagementNotes()    != null ? request.getEngagementNotes()    : "";
        String skillsNotes    = request.getSkillsNotes()        != null ? request.getSkillsNotes()        : "";
        String next           = request.getNextSteps()          != null ? request.getNextSteps()          : "";
        String standard       = request.getCurriculumStandard() != null ? request.getCurriculumStandard() : "";
        String numStudents    = request.getNumberOfStudents()   != null ? request.getNumberOfStudents()   : "";

        String standardDesc = "";
        if (!standard.isEmpty() && standard.contains(":")) {
            standardDesc = standard.substring(standard.indexOf(":") + 1)
                    .replace("]", "").trim();
        }

        // Build group description
        String groupDesc = students;
        if (!numStudents.isEmpty()) {
            groupDesc = "a group of " + numStudents + " students" +
                    (!"Group".equals(students) ? " (" + students + ")" : "");
        }

        return "You are a math tutor writing a group session note for school officials.\n\n" +

                "VOICE & TONE RULES:\n" +
                "- First-person, past tense, casual tutor-to-tutor voice\n" +
                "- Exactly ONE paragraph, no headers, no bullets, no markdown\n" +
                "- Mention specific games and tools BY NAME if listed in skills observations\n" +
                "- Be honest about individual struggles and group wins\n\n" +

                "TONE EXAMPLE (VOICE ONLY — do not copy content):\n" +
                "- \"Today we started with a quick icebreaker, then moved into the main activity. " +
                "The students were engaged overall, though one struggled more than the others. " +
                "We finished with a Blooket and I'll pick up where we left off next session.\"\n\n" +

                "YOUR TASK — Write ONE paragraph weaving BOTH behaviour and skills details naturally:\n\n" +
                "Group: " + groupDesc + "\n" +
                "Engagement Level: " + engagementLvl + "\n" +
                "Math Topic / Standard Focus: " + standardDesc + "\n" +
                "Engagement & Behaviour: " + engagementNotes + "\n" +
                "Skills & Specific Moments: " + skillsNotes + "\n" +
                "Next Steps: " + next + "\n\n" +

                "HARD RULES:\n" +
                "1. Naturally blend behaviour observations with skills/math observations\n" +
                "2. Name Blooket, IXL, Penguin Run, or any game if mentioned in Skills observations\n" +
                "3. Do NOT invent activities or details not in the data above\n" +
                "4. Math concept MUST come from Skills & Specific Moments and Standard Focus only\n" +
                "5. Write exactly one paragraph\n" +
                "6. NEVER quote standard codes — describe concepts in plain language\n" +
                "7. NEVER state engagement as a metric — describe it naturally\n" +
                "8. NEVER mention grade level\n\n" +
                "9. NEVER use 'they', 'them', 'their' to refer to a single individual student — " +
                "instead say 'one student', 'another student', 'this student', or use their name if known\n" +
                "10. 'they/them/their' is ONLY acceptable when referring to the WHOLE GROUP together — " +
                "never for one individual person\n\n"+
                "11. NEVER end with a generic summary sentence like 'Overall it was a productive session' — " +
                "end with something specific about next steps or a specific moment instead\n" +
                "12. Use plain everyday tutor language — avoid formal academic words like " +
                "'automaticity', 'fluency', 'demonstrated proficiency' — say 'nailed it', " +
                "'really got the hang of it', 'crushed it' instead\n\n" +
                "13. Describe ONLY what is in the observations — never invent problems, " +
                "concepts, or mistakes not recorded\n\n" +
                "Write the group session note now:";
    }
}