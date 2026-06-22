package com.example.myservice.dto;

import lombok.Data;

@Data
public class SessionRequest {

    private String tutorName;
    private String sessionDate;
    private String studentName;
    private String subject;        // "Student" or "Overall"
    private String noteType;
    private String districtOrState;
    private String gradeLevel;

    // ✅ SPLIT: was single keyPointers — now two separate fields
    private String engagementNotes;   // Engagement & Behaviour input
    private String skillsNotes;       // Skills & Specific Moments input

    // ✅ Keep for backward compatibility (will be merged from above two)
    private String keyPointers;

    private String nextSteps;
    private String engagement;        // High / Medium / Low radio
    private String curriculumStandard;

    // ✅ NEW: Group Class Note fields
    private String numberOfStudents;  // e.g. "5"
    private String groupLabel;        // e.g. "Group A" (optional)

    private String pronouns; // "he/him", "she/her", "they/them"
}