package com.example.myservice.dto;

public class NoteSummaryDTO {
    private String studentName;
    private String subject;
    private String gradeLevel;
    private String districtOrState;
    private String engagement;
    private String sessionDate;
    private String finalApprovedNote;

    public NoteSummaryDTO() {}

    public NoteSummaryDTO(String studentName, String subject, String gradeLevel,
                          String districtOrState, String engagement,
                          String sessionDate, String finalApprovedNote) {
        this.studentName = studentName;
        this.subject = subject;
        this.gradeLevel = gradeLevel;
        this.districtOrState = districtOrState;
        this.engagement = engagement;
        this.sessionDate = sessionDate;
        this.finalApprovedNote = finalApprovedNote;
    }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(String gradeLevel) { this.gradeLevel = gradeLevel; }

    public String getDistrictOrState() { return districtOrState; }
    public void setDistrictOrState(String districtOrState) { this.districtOrState = districtOrState; }

    public String getEngagement() { return engagement; }
    public void setEngagement(String engagement) { this.engagement = engagement; }

    public String getSessionDate() { return sessionDate; }
    public void setSessionDate(String sessionDate) { this.sessionDate = sessionDate; }

    public String getFinalApprovedNote() { return finalApprovedNote; }
    public void setFinalApprovedNote(String finalApprovedNote) { this.finalApprovedNote = finalApprovedNote; }
}