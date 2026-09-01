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

    /**
     * Parses the raw curriculum-standard string coming from the frontend, which arrives
     * in one of these shapes:
     *   "[Standard 5.NF.A.1: Add and subtract fractions with unlike denominators]"
     *   "[Standard NA: NA]"                         (No Standard Taught)
     *   ""  /  "N/A"                                (legacy / empty)
     *
     * Previously this was parsed independently (and buggily) in both prompt builders by
     * splitting on the FIRST colon without first stripping the "[Standard " wrapper, which
     * left the leading "[Standard " text and a dangling "]" baked into the code
     * (e.g. "[Standard 5.NF.A.1" instead of "5.NF.A.1"). That malformed code didn't match
     * the few-shot example format given to the model, so the model would frequently just
     * drop it from the generated note entirely — this is the root cause of standards being
     * silently omitted from output.
     */
    private static final class ParsedStandard {
        final String code;
        final String description;
        ParsedStandard(String code, String description) {
            this.code = code;
            this.description = description;
        }
    }

    private ParsedStandard parseStandard(String rawStandard) {
        String standard = rawStandard != null ? rawStandard.trim() : "";

        if (standard.isEmpty()
                || standard.equalsIgnoreCase("N/A")
                || standard.toUpperCase(Locale.ROOT).contains("NA: NA")) {
            return new ParsedStandard("", "N/A");
        }

        String inner = standard;
        if (inner.startsWith("[Standard ")) {
            inner = inner.substring("[Standard ".length());
        } else if (inner.startsWith("[")) {
            inner = inner.substring(1);
        }
        if (inner.endsWith("]")) {
            inner = inner.substring(0, inner.length() - 1);
        }
        inner = inner.trim();

        int colonIdx = inner.indexOf(":");
        if (colonIdx >= 0) {
            String code = inner.substring(0, colonIdx).trim();
            String desc = inner.substring(colonIdx + 1).trim();
            return new ParsedStandard(code, desc.isEmpty() ? "N/A" : desc);
        }

        // No colon found — treat the whole thing as a description with no separable code.
        return new ParsedStandard("", inner.isEmpty() ? "N/A" : inner);
    }

    private String buildSingleStudentPrompt(SessionRequest request) {
        String student         = request.getStudentName()        != null ? request.getStudentName()        : "";
        String engagementLvl   = request.getEngagement()         != null ? request.getEngagement()         : "Good";
        String engagementNotes = request.getEngagementNotes()    != null ? request.getEngagementNotes()    : "";
        String skillsNotes     = request.getSkillsNotes()        != null ? request.getSkillsNotes()        : "";
        String next            = request.getNextSteps()          != null ? request.getNextSteps()          : "";
        String pronouns        = request.getPronouns()           != null ? request.getPronouns()           : "they/them";

        ParsedStandard parsed = parseStandard(request.getCurriculumStandard());
        String standardDesc = parsed.description;
        String standardCode = parsed.code;

        return "You are a math tutor writing a brief session note about ONE student. The audience is teachers " +
                "and school officials — peers who already know the standards and curriculum. Write the way a " +
                "tutor talks to another tutor: first-person, conversational, direct, and specific. No formal " +
                "framing for parents. No 'plain-language' explanations of the standard.\n\n" +

                "FORMAT RULES:\n" +
                "- Exactly ONE paragraph.\n" +
                "- First-person ('I', 'me') is expected and fine — you do NOT need to avoid starting with 'I'.\n" +
                "- Use " + student + "'s first name naturally somewhere in the note.\n" +
                "- Past tense.\n" +
                "- No headers, no bullets, no markdown, no bold.\n" +
                "- It is fine — and good — to be honest about hard days, distractions, or behavior. It is also " +
                "fine to be warm and personal when it fits (e.g. \"I missed her so much,\" \"I am so glad she was back\").\n" +
                "- Mention specific concepts, problems, or mistakes " + student + " made, when given in the " +
                "observations below. Don't generalize away from specifics.\n" +
                "- End with what you plan to work on next, OR a clear-eyed observation about a persistent gap, " +
                "when that fits the data. Don't force a moral or an artificial closing sentence if nothing fits.\n" +
                "- LENGTH: 3 to 5 sentences. A session with rich, detailed observations can run toward 5; a " +
                "sparse one should stay closer to 3. Never pad with empty language just to hit a count.\n\n" +

                "TONE CALIBRATION — match the voice of these real tutor notes " +
                "(voice and structure only — do not copy their names, numbers, or specific math content):\n\n" +
                "Example 1: \"Brandon was very distracted today. He left and rejoined the session about 5 times, " +
                "kept getting up out of his seat, and drew on the board after I asked him to stop. I have " +
                "definitely seen better days with him. In terms of the material, he did a great job with " +
                "rounding and the area model! He still needs to work on his 6s, 7s, 8s, 9s multiplication facts " +
                "because he was not sure what 8x6 was.\"\n\n" +
                "Example 2: \"I am so glad Danielle was back at school today! I missed her so much. I caught her " +
                "up on some of the things we did last week and yesterday so she wouldn't be completely lost " +
                "during our session. She was confused about rounding to the nearest 10 at first, so I helped " +
                "her by explaining more in detail and I slowed myself down to make sure she could understand. " +
                "By the end of the session, she did well with generating partial products! I will keep working " +
                "on the area model and rounding tomorrow. It's evident she needs a lot of support in terms of " +
                "fact fluency because she always has to skip count or use repeated addition.\"\n\n" +
                "Example 3: \"Guadalupe did so well today! She demonstrated fantastic understanding of the area " +
                "model, even with 3-digit by 2-digit multiplication. She answered the exit ticket wrong because " +
                "she told me she had a hard time lining up all of the numbers properly. Her final product was " +
                "wrong, but she caught her mistake!\"\n\n" +

                "YOUR TASK — Write ONE paragraph about this specific session, using ONLY the details below:\n\n" +
                "Student Name: " + student + "\n" +
                "Student Pronouns: " + pronouns + "\n" +
                "Engagement Level (context only — do not restate as a label): " + engagementLvl + "\n" +
                "Math Topic (context only — do NOT treat as an observation): " + standardDesc + "\n" +
                "Standard Code: " + (standardCode.isEmpty() ? "N/A" : standardCode) + "\n" +
                "Engagement & Behaviour: " + engagementNotes + "\n" +
                "Skills & Specific Moments: " + skillsNotes + "\n" +
                "⚠ If the observations above are brief (under 10 words total combined), write 2-3 short honest " +
                "sentences only. Do NOT invent detail not explicitly stated.\n" +
                "⚠ If Math Topic is 'N/A', this was a doubt/revision session — do not reference any specific math topic.\n" +
                "Next Steps (tutor-specified, optional): " + next + "\n\n" +

                "HARD RULES:\n" +
                "1. Use specific details from BOTH 'Engagement & Behaviour' AND 'Skills & Specific Moments' when " +
                "both are provided — do not write the note using only one of the two fields.\n" +
                "2. Specific details MUST come ONLY from 'Engagement & Behaviour' and 'Skills & Specific Moments' " +
                "above — never invent an activity, mistake, or moment not stated there.\n" +
                "3. The 'Math Topic' field is background context only — never source specific activities or " +
                "moments from it. Still, name the topic naturally somewhere in the note, but paraphrase it in " +
                "plain, everyday words (e.g. 'we worked on multiplying two-digit numbers'). Do NOT recite or " +
                "closely paraphrase the technical wording of the Math Topic description — that phrasing belongs " +
                "in official standards documents, not in a conversational tutor note.\n" +
                "4. NEVER invent emotional states, confidence levels, or progress not explicitly written in the " +
                "observations. If the tutor didn't write it, it didn't happen. Do not characterize overall " +
                "engagement in your own words (e.g. 'highly engaged', 'very focused') beyond what the stated " +
                "behaviors actually support.\n" +
                "5. NEVER use names other than: " + student + ".\n" +
                "6. Write exactly one paragraph, no line breaks within it.\n" +
                "7. If Standard Code is not 'N/A', include it exactly once in the note, in parentheses right " +
                "after you mention the topic (e.g. \"...we worked on multiplying fractions (5.NF.A.1)...\"). " +
                "Reproduce the Standard Code EXACTLY as given above, character for character. If Standard Code " +
                "is 'N/A', do not mention any code. NEVER mention grade level.\n" +
                "8. Pronouns for " + student + " are: " + pronouns + ". Use ONLY these pronouns. NEVER substitute " +
                "a different pronoun set.\n" +
                "9. NEVER use formal academic language like 'fluency', 'automaticity', 'demonstrated proficiency', " +
                "'procedural fluency' — use plain tutor language instead.\n" +
                "10. If 'Next Steps' was given by the tutor, use it directly for the closing. If it's empty, you " +
                "may still end with a clear-eyed observation about a persistent gap or plan, but ONLY if it's " +
                "directly supported by the stated observations — never invent a plan or gap that wasn't implied " +
                "by what was written. If nothing supports a natural closing, simply end on the last real observation.\n" +
                "11. Avoid vague, content-free closings that don't name anything specific (e.g. 'excited for next " +
                "time' with nothing concrete attached). A specific forward-looking sentence naming an actual " +
                "topic or skill is fine and encouraged when it fits (e.g. 'I'll keep working on the area model " +
                "and rounding tomorrow').\n" +
                "Write the note now:";
    }

    private String buildGroupSessionPrompt(SessionRequest request) {
        String students        = request.getStudentName()        != null ? request.getStudentName()        : "Group";
        String engagementLvl   = request.getEngagement()         != null ? request.getEngagement()         : "Good";
        String engagementNotes = request.getEngagementNotes()    != null ? request.getEngagementNotes()    : "";
        String skillsNotes     = request.getSkillsNotes()        != null ? request.getSkillsNotes()        : "";
        String next            = request.getNextSteps()          != null ? request.getNextSteps()          : "";
        String numStudents     = request.getNumberOfStudents()   != null ? request.getNumberOfStudents()   : "";

        ParsedStandard parsed = parseStandard(request.getCurriculumStandard());
        String standardDesc = parsed.description;
        String standardCode = parsed.code;

        String groupDesc = students;
        if (!numStudents.isEmpty()) {
            groupDesc = "a group of " + numStudents + " students" +
                    (!"Group".equals(students) ? " (" + students + ")" : "");
        }

        return "You are a math tutor writing a brief OVERALL SESSION NOTE about a small group session (multiple " +
                "students together). The audience is teachers and school officials — peers who know the " +
                "standards and curriculum. Write the way a tutor talks to another tutor: first-person, " +
                "conversational, direct, and specific.\n\n" +

                "FORMAT RULES:\n" +
                "- Exactly ONE paragraph.\n" +
                "- First-person ('I', 'we', 'today').\n" +
                "- Past tense.\n" +
                "- About the GROUP as a whole. Use 'the students' or 'they' most of the time.\n" +
                "- It is fine to name individual students when something specific happened to them (a technical " +
                "issue, an absence, a request they made, a notable struggle or moment) — but the note is " +
                "fundamentally about the session, not any one student.\n" +
                "- Mention activities, games, and tools BY NAME when given (IXL, Blooket, ice breakers, exit " +
                "tickets, Penguin Run, math tic-tac-toe, etc.). Specificity matters.\n" +
                "- A natural session-flow narrative works well ('we started with... then we moved on to... we " +
                "finished with...'). Don't force it if the inputs don't suggest a clear order.\n" +
                "- No headers, no bullets, no markdown.\n" +
                "- End with what you plan to work on next OR a clear-eyed observation about a pattern, when it " +
                "fits. Don't force a moral.\n" +
                "- LENGTH: 3 to 4 sentences. Only use a 5th if there is genuinely new information not yet " +
                "covered. Stop the moment all key observations are included.\n\n" +

                "TONE CALIBRATION — match the voice of these real tutor notes " +
                "(voice and structure only — do not copy their names, numbers, or specific content):\n\n" +
                "Example 1: \"In today's session, we finished up multiplication and then moved on to division. " +
                "The students chose to start with a would-you-rather ice breaker question. After, we worked " +
                "through multiplication word problems on IXL which the students did well. As per a student's " +
                "request, I decided they were ready to move on to division. We started division by going over " +
                "how to solve a division problem with remainders. The students needed some help with this but " +
                "started to understand it more. I asked the students to solve a division exit ticket for me, " +
                "and I could tell — because I mentioned we would do a Blooket after and because one student's " +
                "laptop was going to die — they all got the same incorrect answer by rushing. I told them they " +
                "all got it incorrect because they were rushing, so to try again, and they still got incorrect " +
                "answers by rushing. We will continue to go over division next session.\"\n\n" +
                "Example 2: \"Today we did a Level C puzzle, played Penguin Run, practiced estimating 2 by 2 " +
                "digit multiplication, and played a Blooket. The students are still struggling with their " +
                "multiplication facts, but I will encourage them to practice at home. Their levels of " +
                "understanding were mixed on estimation and 2 by 2 digit multiplication — one student seemed " +
                "comfortable and the other struggled with both.\"\n\n" +
                "Example 3: \"Today, we played 'What Number am I' and worked on annotation and multiplication " +
                "word problems. The game went well, and I had the chance to explain what squares are and how " +
                "they relate to multiplication. Then we worked on labelling equations with words to explain the " +
                "symbols the students were seeing — for example, '=' could be written as 'equals' or 'is.' The " +
                "students did well with this part, and we moved on to the word problems. They did well on the " +
                "ones where they were given two factors and asked to find the product, but struggled a bit when " +
                "the question gave one factor and asked to find the missing factor. We finished by playing " +
                "'What Number am I' again, and they did well.\"\n\n" +
                "Example 4: \"We had a good session today going over decimal place value concepts and " +
                "converting fractions to decimals. All of the students logged on a few minutes early, so we " +
                "were able to start the session early! We started with an ice breaker question about money, " +
                "then played a warm-up game of math tic-tac-toe which ended in a tie. After, we started going " +
                "over decimal place value — I asked each student one question about identifying a place value " +
                "in numbers. Then we started working through how to convert fractions out of 100 to decimals. " +
                "Daniel's headset stopped working properly, so a few more minutes of the session than I would " +
                "have liked were taken up trying to solve that issue. Every student was able to answer one " +
                "conversion question, and for the last few minutes we finished with a decimal Blooket. I look " +
                "forward to continuing to work with the students on decimal places.\"\n\n" +

                "YOUR TASK — Write ONE paragraph about this specific session, using ONLY the details below:\n\n" +
                "Group: " + groupDesc + "\n" +
                "Engagement Level (context only — do not restate as a label): " + engagementLvl + "\n" +
                "Math Topic (context only — do NOT treat as an observation): " + standardDesc + "\n" +
                "Standard Code: " + (standardCode.isEmpty() ? "N/A" : standardCode) + "\n" +
                "Engagement & Behaviour: " + engagementNotes + "\n" +
                "Skills & Specific Moments: " + skillsNotes + "\n" +
                "⚠ If Math Topic is 'N/A', this was a doubt/revision session — do not reference any specific math topic.\n" +
                "Next Steps (tutor-specified, optional): " + next + "\n\n" +

                "HARD RULES:\n" +
                "1. Use specific details from BOTH 'Engagement & Behaviour' AND 'Skills & Specific Moments' when " +
                "both are provided — do not write the note using only one of the two fields.\n" +
                "2. Name Blooket, IXL, Penguin Run, or any game/tool if mentioned in the observations.\n" +
                "3. Specific details MUST come ONLY from 'Engagement & Behaviour' and 'Skills & Specific Moments' " +
                "— never invent an activity, mistake, or moment not stated there.\n" +
                "4. The 'Math Topic' field is background context only — never source specific activities or " +
                "moments from it. Still, name the topic naturally somewhere in the note, but paraphrase it in " +
                "plain, everyday words (e.g. 'we worked on two-digit multiplication'). Do NOT recite or closely " +
                "paraphrase the technical wording of the Math Topic description — that phrasing belongs in " +
                "official standards documents, not in a conversational tutor note.\n" +
                "5. NEVER invent activities, methods, emotional states, or details not in the observations. Do " +
                "not characterize overall engagement in your own words beyond what the stated behaviors actually support.\n" +
                "6. Write exactly one paragraph with NO line breaks inside it.\n" +
                "7. If Standard Code is not 'N/A', include it exactly once, in parentheses right after the topic " +
                "is mentioned. Reproduce the Standard Code EXACTLY as given above, character for character. If " +
                "'N/A', omit it entirely. NEVER mention grade level.\n" +
                "8. NEVER use 'they', 'them', or 'their' to refer to a single individual student. Use 'one " +
                "student', 'another student', or 'this student' instead. 'They/them/their' is ONLY acceptable " +
                "when referring to the whole group together.\n" +
                "9. Use plain everyday tutor language — NEVER use 'automaticity', 'fluency', 'demonstrated " +
                "proficiency', 'procedural fluency'.\n" +
                "10. Each observation gets ONE mention only — never loop back or repeat a point already made. " +
                "Once all key moments are covered, stop.\n" +
                "11. If 'Next Steps' was given by the tutor, use it directly for the closing. If it's empty, you " +
                "may still end with a clear-eyed observation about a pattern or plan, but ONLY if it's directly " +
                "supported by the stated observations — never invent one. A specific forward-looking sentence " +
                "naming an actual topic or skill (e.g. 'we will continue going over division next session') is " +
                "fine and encouraged when it fits — just avoid vague, content-free closings that name nothing " +
                "specific.\n" +
                "Write the group session note now:";
    }
}