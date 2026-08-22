package com.example.myservice.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class StudentSummaryDTO {
    private String studentName;
    private long noteCount;
    private String lastSessionDate;

    public StudentSummaryDTO() {}

    public StudentSummaryDTO(String studentName, long noteCount, String lastSessionDate) {
        this.studentName = studentName;
        this.noteCount = noteCount;
        this.lastSessionDate = lastSessionDate;
    }

}