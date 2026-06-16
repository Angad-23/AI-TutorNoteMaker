//package com.example.myservice.service;
//
//import com.example.myservice.dto.SessionRequest;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.*;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.*;
//
//@Service
//public class OpenAiService {
//
//    @Value("${openai.api.key}")
//    private String API_KEY;
//
//    @Value("${openai.api.url}")
//    private String API_URL;
//
//    public String generateSessionNote(SessionRequest request) {
//        RestTemplate restTemplate = new RestTemplate();
//        String promptBody;
//
//        // Smart Backend Mapping Check for Dynamic Prompt Determination
//        String targetSubject = request.getSubject() != null ? request.getSubject().trim().toLowerCase() : "";
//
//        if (targetSubject.contains("overall") || targetSubject.contains("group")) {
//            promptBody = buildGroupSessionPrompt(request);
//        } else {
//            promptBody = buildSingleStudentPrompt(request);
//        }
//
//        Map<String, Object> requestBody = new HashMap<>();
//        requestBody.put("model", "llama-3.1-8b-instant");
//        requestBody.put("max_tokens", 500);
//        requestBody.put("temperature", 0.4); // Stable calibration variance
//
//        List<Map<String, String>> messages = new ArrayList<>();
//        messages.add(Map.of("role", "user", "content", promptBody));
//        requestBody.put("messages", messages);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.setBearerAuth(API_KEY);
//
//        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
//
//        try {
//            ResponseEntity<Map> response = restTemplate.postForEntity(API_URL, entity, Map.class);
//            Map<?, ?> responseBody = response.getBody();
//
//            if (responseBody != null && responseBody.containsKey("choices")) {
//                List<?> choices = (List<?>) responseBody.get("choices");
//                if (!choices.isEmpty()) {
//                    Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
//                    Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
//                    if (message != null && message.containsKey("content")) {
//                        return (String) message.get("content");
//                    }
//                }
//            }
//            return "Error: Unexpected response format from AI server.";
//        } catch (Exception e) {
//            return "Error generating note: " + e.getMessage();
//        }
//    }
//
//    private String buildSingleStudentPrompt(SessionRequest request) {
//        return String.format(
//                "You are a math tutor writing a brief session note about ONE student. " +
//                        "The audience is teachers and school officials — peers who already know the standards and curriculum. " +
//                        "Write the way a tutor talks to another tutor: first-person, conversational, direct, and specific. " +
//                        "No formal framing for parents. No 'plain-language' explanations of the standard.\n\n" +
//                        "Format rules:\n" +
//                        "- Exactly ONE paragraph.\n" +
//                        "- First-person (\"I\", \"me\").\n" +
//                        "- Use the student's first name naturally.\n" +
//                        "- Past tense for the session.\n" +
//                        "- NO headers, NO bullets, NO markdown formatting at all.\n" +
//                        "- It is fine — and good — to be honest about hard days, distractions, or behavior. It is also fine to be warm and personal when it fits.\n" +
//                        "- Mention specific concepts, problems, or mistakes the student made when given. Don't generalize.\n" +
//                        "- End with what you plan to work on next OR a clear-eyed observation about a persistent gap, when that fits the data. Don't force a moral.\n\n" +
//                        "Tone calibration — match these real examples:\n" +
//                        "Example 1: \"Brandon was very distracted today. He left and rejoined the session about 5 times, kept getting up out of his seat, and drew on the board after I asked him to stop. I have definitely seen better days with him. I hope gets better. In terms of the material, he did a great job with rounding and the area model! He still needs to work on his 6s, 7s, 8s, 9s multiplication facts because he was not sure what 8x6 was.\"\n" +
//                        "Example 2: \"I am so glad Danielle was back at school today! I missed her so much. I caught her up on some of things we did last week and yesterday so she wouldn't be completely lost during our session. She was confused about rounding to the nearest 10 at first, so I helped her by explaining more in detail and I slowed myself down to make sure she could understand. By the end of the session, she did well with generating partial products! I will keep working on the area model and rounding tomorrow. It's evident she needs a lot of support in terms of fact fluency because she always has to skip count or use repeated addition.\"\n" +
//                        "Example 3: \"Guadalupe did so well today! She demonstrated fantastic understanding of the area model, even with 3-digit by 2-digit multiplication. She answered the exit ticket wrong because she told me she had a hard time lining up all of the numbers properly. Her final product was wrong, but she caught her mistake!\"\n\n" +
//                        "### INPUT INPUTS:\n" +
//                        "- Student Name: %s\n" +
//                        "- Session Date: %s\n" +
//                        "- District / State Context: %s (Grade: %s)\n" +
//                        "- Subject / Topic Target: %s\n" +
//                        "- Engagement and Behaviour Profile: %s\n" +
//                        "- Key Observations (Pointers & Next Steps): %s, %s\n\n" +
//                        "Write the exact single student note paragraph now:",
//                request.getStudentName(), request.getSessionDate(), request.getDistrictOrState(), request.getGradeLevel(),
//                request.getSubject(), request.getEngagement(), request.getKeyPointers(), request.getNextSteps()
//        );
//    }
//
//
//    private String buildGroupSessionPrompt(SessionRequest request) {
//        return String.format(
//                "You are a math tutor writing a brief OVERALL SESSION NOTE about a small group session (multiple students together). " +
//                        "The audience is teachers and school officials — peers who know the standards and curriculum. " +
//                        "Write the way a tutor talks to another tutor: first-person, conversational, direct, and specific.\n\n" +
//                        "Format rules:\n" +
//                        "- Exactly ONE paragraph.\n" +
//                        "- First-person (\"I\", \"we\", \"today\").\n" +
//                        "- Past tense.\n" +
//                        "- About the GROUP as a whole. Use \"the students\" or \"they\" most of the time.\n" +
//                        "- It is fine to name individual students when something specific happened to them (a technical issue, an absence, a request they made, a notable struggle or moment) — but the note is fundamentally about the session, not any one student.\n" +
//                        "- Mention activities, games, and tools BY NAME when given (IXL, Blooket, ice breakers, exit tickets, Penguin Run, math tic-tac-toe, etc.). Specificity matters.\n" +
//                        "- A natural session-flow narrative works well (\"we started with... then we moved on to... we finished with...\"). Don't force it if the inputs don't suggest a clear order.\n" +
//                        "- NO headers, NO bullets, NO markdown formatting at all.\n" +
//                        "- End with what you plan to work on next OR a clear-eyed observation about a pattern, when it fits. Don't force a moral.\n\n" +
//                        "Tone calibration — match these real examples:\n" +
//                        "Example 1: \"In today's session, we finished up multiplication and then moved on to division. The students chose to start with a would you rather ice breaker question. After, we worked through multiplication word problems on IXL which the students did well. As per a student's request, I decided they were ready to move on to division. We started division by going over how to solve a division problem with remainders. The students needed some help with this but started to understand it more. I asked the students to solve a division exit ticket for me, and I could tell because I mentioned we would do a Blooket after and because one student's laptop was going to die, they all got the same incorrect answer. I told them that they all got it incorrect because they were rushing so to try again, and they all still got the incorrect answers by rushing. We will continue to go over division next session.\"\n" +
//                        "Example 2: \"Today we did a Level C puzzle, played Penguin Run, practiced estimating 2 by 2 digit multiplication, and played a Blooket. The students are still struggling with their multiplication facts, but I will encourage them to practice at home. Their levels of understanding were mixed on estimation and 2 by 2 digit multiplication. One student seemed comfortable and the other struggled with both. Afterwards, we played a Blooket.\"\n" +
//                        "Example 3: \"Today, we played 'What Number am I' and worked on annotation and multiplication word problems. The game went well, and I had the chance to explain what squares are and how they relate to multiplication. Then we worked on labelling equations with words to explain the symbols the students were seeing. For example, '=' could be written with words as 'equals' or 'is.' The students did well with this part, and we moved on to the word problems. They did well on the ones where they were given two factors and asked to find the product, but struggled a bit when the question gave one factor and asked to find the missing factor. We finished by playing 'What Number am I' again, and they did well.\"\n" +
//                        "Example 4: \"We had a good session today going over over Decimal Place Value Concepts and Converting Fractions to Decimals. All of the students logged on a few minutes early, so we were able to start the session early!. We started with an ice breaker question about money. Then, we played a warm-up game of math tic tac toe which ended in a tie. After, we started going over decimal place value. I asked each student one question about identifying a place value in numbers. After, we started working through how to convert fractions out of 100 to decimals. Daniel's headset stopped working properly, so a few more minutes of the session than I would have liked were taken up trying to solve that issue. Every student was able to answer one conversion question, and for the last few minutes we finished with a decimal Blooket. I look forward to continuing to work with the students on decimal places.\"\n\n" +
//                        "### INPUT INPUTS:\n" +
//                        "- Group Student Names: %s\n" +
//                        "- Session Date: %s\n" +
//                        "- District / State Context: %s (Grade: %s)\n" +
//                        "- Engagement and Behaviour Profile: %s\n" +
//                        "- Key Observations (Pointers & Next Steps): %s, %s\n\n" +
//                        "Write the exact group session note paragraph now:",
//                request.getStudentName(), request.getSessionDate(), request.getDistrictOrState(), request.getGradeLevel(),
//                request.getEngagement(), request.getKeyPointers(), request.getNextSteps()
//        );
//    }
//}
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
        // 🌟 THE TRUTH DETECTOR
        System.out.println("=== DATA RECEIVED FROM UI ===");
        System.out.println("Student: " + request.getStudentName());
        System.out.println("Pointers: " + request.getKeyPointers());
        System.out.println("=============================");

        RestTemplate restTemplate = new RestTemplate();
        String promptBody;

        // Smart Backend Mapping Check for Dynamic Prompt Determination
        String targetSubject = request.getSubject() != null ? request.getSubject().trim().toLowerCase() : "";

        if (targetSubject.contains("overall") || targetSubject.contains("group")) {
            promptBody = buildGroupSessionPrompt(request);
        } else {
            promptBody = buildSingleStudentPrompt(request);
        }

//        Map<String, Object> requestBody = new HashMap<>();
//        requestBody.put("model", "llama-3.1-8b-instant");
//        requestBody.put("max_tokens", 500);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "llama-3.1-8b-instant");
        requestBody.put("max_tokens", 150);  // 🌟 CRITICAL LOWERING: Prevents the model from rambling or regenerating example text
        requestBody.put("temperature", 0.0); // 🌟 ABSOLUTE ZERO: Forces strict mathematical decoding of your input parameters

        // 🌟 CRITICAL FIX: Dropped to 0.1 to eliminate hallucination drift entirely
//        requestBody.put("temperature", 0.1);

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
        // 🌟 FAST FIX: Check both fields so your standard data is never lost or null
        String topic = "Math Standards";
        if (request.getSubject() != null && !request.getSubject().isBlank()) {
            topic = request.getSubject();
        } else if (request.getNoteType() != null && !request.getNoteType().isBlank()) {
            topic = request.getNoteType();
        }

        String engagement = request.getEngagement() != null ? request.getEngagement() : "Good engagement";
        String pointers = request.getKeyPointers() != null ? request.getKeyPointers() : "";
        String next = request.getNextSteps() != null ? request.getNextSteps() : "";

        return String.format(
                "You are a professional math tutor writing a concise session note about ONE student for school officials.\n\n" +
                        "CORE TEXT ARCHITECTURE RULE:\n" +
                        "The note must make clear what specific math skill or standard was the focus of the session, " +
                        "but describe it the way a tutor naturally talks — in plain, casual language — NOT by quoting the formal standard text verbatim. " +
                        "For example, instead of saying 'rounding whole numbers from 0 to 10,000 to the nearest 10, 100 or 1,000', " +
                        "say something like 'rounding numbers to the nearest 10, 100, or 1,000' or 'practicing rounding'. " +
                        "You may mention the state and grade briefly if it fits naturally, but do not force a rigid opening sentence like 'We focused on %s %s standards for %s.'\n\n" +
                        "⚠️ MANDATORY TONE & STYLE HEDGE:\n" +
                        "Do NOT copy any text, names, or math facts from the calibration guide below. " +
                        "Only match the first-person pacing, exclamation use, and casual tutor-to-tutor voice.\n\n" +
                        "<style-examples>\n" +
                        "Example 1: \"Brandon was very distracted today. He left and rejoined the session about 5 times, kept getting up out of his seat, and drew on the board after I asked him to stop. I have definitely seen better days with him. I hope gets better. In terms of the material, he did a great job with rounding and the area model! He still needs to work on his 6s, 7s, 8s, 9s multiplication facts because he was not sure what 8x6 was.\"\n\n" +
                        "Example 2: \"I am so glad Danielle was back at school today! I missed her so much. I caught her up on some of things we did last week and yesterday so she wouldn't be completely lost during our session. She was confused about rounding to the nearest 10 at first, so I helped her by explaining more in detail and I slowed myself down to make sure she could understand. By the end of the session, she did well with generating partial products! I will keep working on the area model and rounding tomorrow. It's evident she needs a lot of support in terms of fact fluency because she always has to skip count or use repeated addition.\"\n\n" +
                        "Example 3: \"Guadalupe did so well today! She demonstrated fantastic understanding of the area model, even with 3-digit by 2-digit multiplication. She answered the exit ticket wrong because she told me she had a hard time lining up all of the numbers properly. Her final product was wrong, but she caught her mistake!\"\n\n" +
                        "Example 4: \"Maria was super engaged today, which made the session a lot of fun. She did a great job with rounding whole numbers, especially when we practiced rounding 6,847 to the nearest 100 - she nailed it. However, she initially struggled with rounding to the nearest 1,000, keeping her focus on the hundreds digit instead of the thousands digit. With some extra practice problems, she started to get the hang of it by the end of the session. I'm looking forward to seeing her progress in this area.\"\n" +
                        "</style-examples>\n\n" +
                        "Now, construct the single paragraph using 100%% of the specific facts provided in this data block:\n" +
                        "<session-data>\n" +
                        "- Student Name: %s\n" +
                        "- Date: %s\n" +
                        "- Location & Grade: %s (Grade %s)\n" +
                        "- Compulsory Topic Focus: %s\n" +
                        "- Vibe/Engagement: %s\n" +
                        "- Observations: %s\n" +
                        "- Next Steps: %s\n" +
                        "</session-data>\n\n" +
                        "Write the concise integrated note paragraph now (No markdown headers, no bold text):",
                request.getDistrictOrState(), request.getGradeLevel(), topic, // For the example sentence injection
                request.getStudentName(), request.getSessionDate(), request.getDistrictOrState(), request.getGradeLevel(),
                topic, engagement, pointers, next
        );
    }

    private String buildGroupSessionPrompt(SessionRequest request) {
        String topic = request.getSubject() != null ? request.getSubject() : "Group Math Concepts";
        String engagement = request.getEngagement() != null ? request.getEngagement() : "Good group participation";
        String pointers = request.getKeyPointers() != null ? request.getKeyPointers() : "";
        String next = request.getNextSteps() != null ? request.getNextSteps() : "";

        return String.format(
                "You are a math tutor writing a brief OVERALL SESSION NOTE about a small group session. " +
                        "The audience is teachers and school officials — peers who know the standards and curriculum. " +
                        "Write the way a tutor talks to another tutor: first-person, conversational, direct, and specific.\n\n" +
                        "Format rules:\n" +
                        "- Exactly ONE paragraph.\n" +
                        "- First-person (\"I\", \"we\", \"today\").\n" +
                        "- Past tense.\n" +
                        "- Focus heavily on activities, games, and tools BY NAME when passed in the raw pointers (IXL, Blooket, ice breakers, Penguin Run, etc.).\n" +
                        "- NO headers, NO bullets, NO markdown formatting at all.\n\n" +
                        "⚠️ STRICT CONTENT CONSTRAINT:\n" +
                        "Do NOT include math actions, issues, or details from the examples below unless they are explicitly present inside the <session-data> collection block. " +
                        "The examples inside <style-examples> are exclusively for pacing guidance.\n\n" +
                        "<style-examples>\n" +
                        "Example 1: \"In today's session, we finished up multiplication and then moved on to division. The students chose to start with a would you rather ice breaker question. After, we worked through multiplication word problems on IXL which the students did well. As per a student's request, I decided they were ready to move on to division. We started division by going over how to solve a division problem with remainders. The students needed some help with this but started to understand it more. I asked the students to solve a division exit ticket for me, and I could tell because I mentioned we would do a Blooket after and because one student's laptop was going to die, they all got the same incorrect answer. I told them that they all got it incorrect because they were rushing so to try again, and they all still got the incorrect answers by rushing. We will continue to go over division next session.\"\n\n" +
                        "Example 2: \"Today we did a Level C puzzle, played Penguin Run, practiced estimating 2 by 2 digit multiplication, and played a Blooket. The students are still struggling with their multiplication facts, but I will encourage them to practice at home. Their levels of understanding were mixed on estimation and 2 by 2 digit multiplication. One student seemed comfortable and the other struggled with both. Afterwards, we played a Blooket.\"\n\n" +
                        "Example 3: \"Today, we played 'What Number am I' and worked on annotation and multiplication word problems. The game went well, and I had the chance to explain what squares are and how they relate to multiplication. Then we worked on labelling equations with words to explain the symbols the students were seeing. For example, '=' could be written with words as 'equals' or 'is.' The students did well with this part, and we moved on to the word problems. They did well on the ones where they were given two factors and asked to find the product, but struggled a bit when the question gave one factor and asked to find the missing factor. We finished by playing 'What Number am I' again, and they did well.\"\n\n" +
                        "Example 4: \"We had a good session today going over over Decimal Place Value Concepts and Converting Fractions to Decimals. All of the students logged on a few minutes early, so we were able to start the session early!. We started with an ice breaker question about money. Then, we played a warm-up game of math tic tac toe which ended in a tie. After, we started going over decimal place value. I asked each student one question about identifying a place value in numbers. After, we started working through how to convert fractions out of 100 to decimals. Daniel's headset stopped working properly, so a few more minutes of the session than I would have liked were taken up trying to solve that issue. Every student was able to answer one conversion question, and for the last few minutes we finished with a decimal Blooket. I look forward to continuing to work with the students on decimal places.\"\n" +
                        "</style-examples>\n\n" +
                        "Now, extract the facts from the user data block below and write a custom narrative paragraph using only these details:\n\n" +
                        "<session-data>\n" +
                        "- Group Student Names: %s\n" +
                        "- Session Date: %s\n" +
                        "- District / State Context: %s (Grade: %s)\n" +
                        "- Curricular Topic Goal: %s\n" +
                        "- Engagement and Behaviour Profile: %s\n" +
                        "- Key Observations (Pointers): %s\n" +
                        "- Next Steps Plan: %s\n" +
                        "</session-data>\n\n" +
                        "Write the exact group session note paragraph now:",
                request.getStudentName(), request.getSessionDate(), request.getDistrictOrState(), request.getGradeLevel(),
                topic, engagement, pointers, next
        );
    }
}