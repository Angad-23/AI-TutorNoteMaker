package com.example.myservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "session_notes")
@Data
public class SessionNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String studentName;

    // 🌟 THE JPA BRIDGE: This maps your Java 'subject' property to the new 'note_type' DB column
    @Column(name = "note_type")
    private String subject;

    private String gradeLevel;
    private String engagement;
    private String tutorName;
    private String sessionDate;

    @Column(columnDefinition = "text")
    private String rawPointers;

    @Column(columnDefinition = "text")
    private String finalApprovedNote;

    private String districtOrState; // Tracking regional compliance
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
    }

    // Stores the selected curriculum standard
    @Column(name = "curriculum_standard", columnDefinition = "text")
    private String curriculumStandard;


}