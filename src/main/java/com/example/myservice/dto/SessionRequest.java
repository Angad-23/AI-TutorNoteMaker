package com.example.myservice.dto;

import lombok.Data;

@Data
public class SessionRequest {
    private String studentName;
    private String subject;
    private String gradeLevel;
    private String engagement;
    private String keyPointers;
    private String nextSteps;
    private String districtOrState;

    // ADD THESE TWO NEW LINES
    private String tutorName;
    private String sessionDate;
}