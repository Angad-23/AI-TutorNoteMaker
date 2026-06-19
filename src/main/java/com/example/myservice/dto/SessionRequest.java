package com.example.myservice.dto;

import lombok.Data;

@Data
public class SessionRequest {
    private String studentName;
    private String subject;          // Mapped to Note Type ("Student" or "Overall")
    private String gradeLevel;       // Mapped to Grade dropdown (Elementary, Middle, High)
    private String districtOrState;  // Mapped to State dropdown (Maryland, Florida, General)
    private String engagement;       // Mapped to Radio buttons (High, Medium, Low)
    private String keyPointers;      // Mapped to text area (Parameters/Observations)
    private String nextSteps;        // Mapped to Next Steps / Homework input text field
    private String tutorName;        // Mapped to Tutor Name text input
    private String sessionDate;      // Mapped to Date selector
    private String noteType;
    private String curriculumStandard;

}