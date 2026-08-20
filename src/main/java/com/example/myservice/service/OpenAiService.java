package com.example.myservice.service;

import java.util.*;

import com.example.myservice.dto.SessionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Service
public class OpenAiService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiService.class);

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
        requestBody.put("model", "openai/gpt-oss-120b");
        requestBody.put("max_tokens", 800);
        requestBody.put("temperature", 0.1);
        requestBody.put("reasoning_effort", "low");
        

        List<Map<String, String>> messages = new ArrayList<>();

        messages.add(Map.of(
                "role", "system",
                "content", "You are a math tutor writing professional session notes. " +
                        "You follow all instructions exactly as written. " +
                        "You NEVER invent details not explicitly given to you. " +
                        "You write concisely — stop when the observations are covered, " +
                        "between 3 and 5 sentences. Never pad or repeat to reach a sentence count. " +
                        "A short accurate note is always better than a long padded one."
        ));

        messages.add(Map.of("role", "user", "content", promptBody));
        requestBody.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(API_KEY);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(API_URL, entity, Map.class);
            Map<?, ?> responseBody = response.getBody();

            // to check the response from the prompts given
//            System.out.println("GROQ RAW RESPONSE: " + responseBody);

            if (responseBody != null && responseBody.containsKey("choices")) {
                List<?> choices = (List<?>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
                    Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
//                    if (message != null && message.containsKey("content")) {
//                        return (String) message.get("content");
//                    }
                    if (message != null && message.containsKey("content")) {
                        String rawContent = (String) message.get("content");
                        return stripThinkingBlock(rawContent);
                    }
                }
            }
            return "Error: Unexpected response format from AI server.";
        } catch (Exception e) {
            log.error("Error calling Groq API: {}", e.getMessage(), e);
            return "Error generating note: " + e.getMessage();
        }
    }

    private String stripThinkingBlock(String content) {
        if (content == null) return content;
        return content.replaceAll("(?s)<think>.*?</think>", "").trim();
    }

    private String buildSingleStudentPrompt(SessionRequest request) {
        String student         = request.getStudentName()        != null ? request.getStudentName()        : "";
        String engagementLvl   = request.getEngagement()         != null ? request.getEngagement()         : "Good";
        String engagementNotes = request.getEngagementNotes()    != null ? request.getEngagementNotes()    : "";
        String skillsNotes     = request.getSkillsNotes()        != null ? request.getSkillsNotes()        : "";
        String next            = request.getNextSteps()          != null ? request.getNextSteps()          : "";
        String standard        = request.getCurriculumStandard() != null ? request.getCurriculumStandard() : "";
        String pronouns        = request.getPronouns()           != null ? request.getPronouns()           : "they/them";

        String standardDesc;
        if (standard.isEmpty() || standard.equals("N/A") || standard.contains("NA: NA")) {
            standardDesc = "N/A";
        } else if (standard.contains(":")) {
            standardDesc = standard.substring(standard.indexOf(":") + 1)
                    .replace("]", "").trim();
        } else {
            standardDesc = standard;
        }

        return "You are a professional math tutor writing a session note for school officials.\n\n" +

                "VOICE & TONE RULES:\n" +
                "- First-person, past tense, casual tutor-to-tutor voice\n" +
                "- Exactly ONE paragraph, no headers, no bullets, no markdown, no bold\n" +
                "- Sound warm, honest, and specific\n" +
                "- Do NOT start with 'I' — start with the student's name\n" +
                "- LENGTH: 3 to 5 sentences. Stop as soon as all key observations are covered. " +
                "3 strong sentences is better than 5 where the last two repeat or add nothing new.\n\n" +

                "TONE EXAMPLES (VOICE ONLY — do not copy any math content or names):\n" +
                "- \"[Student] was really engaged today and picked up the concept quickly. " +
                "He struggled a bit at first but by the end had a solid grasp of it. " +
                "Next time we'll move on to the harder problems.\"\n" +
                "- \"[Student] had a tough session today. She was distracted but still managed " +
                "to get through the material. I'll revisit this next time before moving on.\"\n\n" +

                "YOUR TASK — Write ONE paragraph weaving BOTH behaviour and skills details naturally:\n\n" +
                "Student Name: " + student + "\n" +
                "Student Pronouns: " + pronouns + "\n" +
                "Engagement Level: " + engagementLvl + "\n" +
                "Math Topic (context only — do NOT treat as an observation): " + standardDesc + "\n" +
                "Engagement & Behaviour: " + engagementNotes + "\n" +
                "Skills & Specific Moments: " + skillsNotes + "\n" +
                "⚠ If the observations above are brief (under 10 words total), write 2-3 short honest sentences only. " +
                "Do NOT add any detail not explicitly stated.\n" +
                "⚠ If Math Topic is 'N/A', this was a doubt/revision session — do not reference any specific math topic.\n" +
                "Next Steps: " + next + "\n\n" +

                "HARD RULES:\n" +
                "1. Naturally blend the behaviour observations with the skills/math observations in one flowing paragraph.\n" +
                "2. Specific details MUST come ONLY from 'Engagement & Behaviour' and 'Skills & Specific Moments' above.\n" +
                "3. The 'Math Topic' field is background context only — NEVER use it as a source of specific activities, methods, or moments.\n" +
                "4. NEVER invent emotional states, confidence levels, or progress not explicitly written in the observations. If the tutor didn't write it, it didn't happen.\n" +
                "5. NEVER use names other than: " + student + ".\n" +
                "6. Write exactly one paragraph, no line breaks within it.\n" +
                "7. Write like talking to a colleague — contractions, warmth, personality.\n" +
                "8. LENGTH: Write between 3 and 5 sentences. Stop as soon as all key observations are covered. Do NOT pad to reach 5.\n" +
                "9. NEVER mention grade level or standard codes.\n" +
                "10. NEVER state engagement as a metric — weave it naturally into the narrative.\n" +
                "11. Pronouns for " + student + " are: " + pronouns + ". Use ONLY these pronouns. NEVER substitute a different pronoun set.\n" +
                "12. Reference the math topic conversationally — never explain or quote the standard description word for word.\n" +
                "13. NEVER use formal academic language like 'fluency', 'automaticity', 'demonstrated proficiency', 'procedural fluency' — use plain tutor language instead.\n" +
                "14. NEVER end with a generic closing sentence. Banned phrases: " +
                "'I'm looking forward to', 'I look forward to', 'can't wait to see', " +
                "'build on this momentum', 'build on what they've learned', 'back to the drawing board', " +
                "'excited for next session', 'see how they do next time', 'after mastering the basics', " +
                "'now that she's got a handle on', 'confidence grew', 'taking on each new challenge'. " +
                "End on a specific observed moment or a concrete next step instead.\n" +
                "Write the note now:";
    }

    private String buildGroupSessionPrompt(SessionRequest request) {
        String students        = request.getStudentName()        != null ? request.getStudentName()        : "Group";
        String engagementLvl   = request.getEngagement()         != null ? request.getEngagement()         : "Good";
        String engagementNotes = request.getEngagementNotes()    != null ? request.getEngagementNotes()    : "";
        String skillsNotes     = request.getSkillsNotes()        != null ? request.getSkillsNotes()        : "";
        String next            = request.getNextSteps()          != null ? request.getNextSteps()          : "";
        String standard        = request.getCurriculumStandard() != null ? request.getCurriculumStandard() : "";
        String numStudents     = request.getNumberOfStudents()   != null ? request.getNumberOfStudents()   : "";

        String standardDesc;
        if (standard.isEmpty() || standard.equals("N/A") || standard.contains("NA: NA")) {
            standardDesc = "N/A";
        } else if (standard.contains(":")) {
            standardDesc = standard.substring(standard.indexOf(":") + 1)
                    .replace("]", "").trim();
        } else {
            standardDesc = standard;
        }

        String groupDesc = students;
        if (!numStudents.isEmpty()) {
            groupDesc = "a group of " + numStudents + " students" +
                    (!"Group".equals(students) ? " (" + students + ")" : "");
        }

        return "You are a math tutor writing a group session note for school officials.\n\n" +

                "VOICE & TONE RULES:\n" +
                "- First-person, past tense, casual tutor-to-tutor voice\n" +
                "- Exactly ONE continuous paragraph, no headers, no bullets, no markdown, no line breaks inside\n" +
                "- Mention specific games and tools BY NAME if listed in observations\n" +
                "- Be honest about individual struggles and group wins\n" +
                "- LENGTH: 3 to 4 sentences. Only use a 5th if there is genuinely new information not yet covered. " +
                "Stop the moment all key observations are included.\n\n" +

                "TONE EXAMPLE (VOICE ONLY — do not copy content):\n" +
                "- \"Today we warmed up with a Blooket round on times tables and all four students did well. " +
                "We moved into the main topic and most picked it up quickly, though one student kept making " +
                "carrying errors so I'll work with that student individually next time.\"\n\n" +

                "YOUR TASK — Write ONE paragraph weaving BOTH behaviour and skills details naturally:\n\n" +
                "Group: " + groupDesc + "\n" +
                "Engagement Level: " + engagementLvl + "\n" +
                "Math Topic (context only — do NOT treat as an observation): " + standardDesc + "\n" +
                "Engagement & Behaviour: " + engagementNotes + "\n" +
                "Skills & Specific Moments: " + skillsNotes + "\n" +
                "⚠ If Math Topic is 'N/A', this was a doubt/revision session — do not reference any specific math topic.\n" +
                "Next Steps: " + next + "\n\n" +

                "HARD RULES:\n" +
                "1. Naturally blend behaviour observations with skills/math observations.\n" +
                "2. Name Blooket, IXL, Penguin Run, or any game/tool if mentioned in the observations.\n" +
                "3. Specific details MUST come ONLY from 'Engagement & Behaviour' and 'Skills & Specific Moments'.\n" +
                "4. The 'Math Topic' field is background context only — NEVER use it as a source of specific moments or activities.\n" +
                "5. NEVER invent activities, methods, or details not in the observations.\n" +
                "6. Write exactly one paragraph with NO line breaks inside it.\n" +
                "7. NEVER quote standard codes — describe concepts in plain everyday language.\n" +
                "8. NEVER state engagement as a metric — describe it naturally in the narrative.\n" +
                "9. NEVER mention grade level.\n" +
                "10. NEVER use 'they', 'them', or 'their' to refer to a single individual student. " +
                "Use 'one student', 'another student', or 'this student' instead. " +
                "'They/them/their' is ONLY acceptable when referring to the whole group together.\n" +
                "11. NEVER end with a generic closing sentence. Banned phrases: " +
                "'Overall it was a great session', 'I'm looking forward to', 'I look forward to', " +
                "'build on this momentum', 'build on what they learned', 'can't wait to see', " +
                "'excited for next session', 'see how they do'. " +
                "End on a specific next step or a specific moment from the session instead.\n" +
                "12. Use plain everyday tutor language — NEVER use 'automaticity', 'fluency', " +
                "'demonstrated proficiency', 'procedural fluency'. Say 'nailed it', " +
                "'really got the hang of it', 'clicked for them', 'crushed it' instead.\n" +
                "13. Do NOT start the note with 'I' — start with the group name or 'Today' or 'We'.\n" +
                "14. Each observation gets ONE mention only — never loop back or repeat a point already made.\n" +
                "15. Once you have covered all key moments, STOP. Do not add a sentence that rephrases something already said.\n" +
                "Write the group session note now:";
    }
}