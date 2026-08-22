package com.example.myservice.controller;

import com.example.myservice.dto.SessionRequest;
import com.example.myservice.entity.SessionNote;
import com.example.myservice.repository.SessionNoteRepository;
import com.example.myservice.service.ExcelExportService;
import com.example.myservice.service.OpenAiService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import com.example.myservice.repository.TutorUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/notes")
public class NotesController {

    private static final Logger log = LoggerFactory.getLogger(NotesController.class);
    private final OpenAiService openAiService;
    private final SessionNoteRepository noteRepository;
    private final TutorUserRepository tutorUserRepository;
    private final ExcelExportService excelExportService;


    public NotesController(OpenAiService openAiService,
                           SessionNoteRepository noteRepository,
                           TutorUserRepository tutorUserRepository, ExcelExportService excelExportService) {
        this.openAiService = openAiService;
        this.noteRepository = noteRepository;
        this.tutorUserRepository = tutorUserRepository;
        this.excelExportService = excelExportService;

    }

    // Get logged-in tutor's full name
    private String getLoggedInTutorName(UserDetails userDetails) {
        if (userDetails != null) {
            return tutorUserRepository.findByUsername(userDetails.getUsername())
                    .map(tutor -> tutor.getFullName())
                    .orElse("Unknown Tutor");
        }
        return "Unknown Tutor";
    }

    // GET: Main form
    @GetMapping
    public String showForm(@AuthenticationPrincipal UserDetails userDetails,
                           @RequestParam(value = "success", required = false) String success,
                           Model model,
                           HttpSession session) {

        String generatedNote = (String) session.getAttribute("generatedNote");
        SessionRequest sessionRequest = (SessionRequest) session.getAttribute("lastSessionRequest");

        if (sessionRequest == null) {
            sessionRequest = new SessionRequest();
        }

        String currentTutor = getLoggedInTutorName(userDetails);
        sessionRequest.setTutorName(currentTutor);

        if (generatedNote != null) {
            model.addAttribute("generatedNote", generatedNote);
            session.removeAttribute("generatedNote");
            session.removeAttribute("lastSessionRequest");
        }

        model.addAttribute("sessionRequest", sessionRequest);
        model.addAttribute("savedNotes",
                noteRepository.findTop5ByTutorNameOrderByCreatedAtDesc(currentTutor));

        if ("true".equals(success)) model.addAttribute("saveSuccess", true);
        return "tutor-form";
    }

    // ✅ GET: Full history
    @GetMapping("/history")
    public String viewNotesHistory(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String currentTutor = getLoggedInTutorName(userDetails);
        List<SessionNote> savedNotes = noteRepository.findAllByTutorNameOrderByCreatedAtDesc(currentTutor);
        model.addAttribute("savedNotes", savedNotes);
        model.addAttribute("fullName", currentTutor);
        return "notes-history";
    }

    // ✅ GET: Export saved notes to Excel
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportToExcel(@AuthenticationPrincipal UserDetails userDetails) throws IOException {
        String currentTutor = getLoggedInTutorName(userDetails);
        List<SessionNote> notes = noteRepository.findAllByTutorNameOrderByCreatedAtDesc(currentTutor);

        byte[] excelBytes = excelExportService.exportSessionNotes(notes);

        String safeName = currentTutor.replaceAll("[^a-zA-Z0-9]+", "_");
        String filename = "session-notes-" + safeName + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    // ✅ POST: Generate note
    @PostMapping("/generate")
    public String generateNote(@AuthenticationPrincipal UserDetails userDetails,
                               @ModelAttribute SessionRequest sessionRequest,
                               HttpSession session) {

        // ✅ Clear previous session data
        session.removeAttribute("generatedNote");
        session.removeAttribute("lastSessionRequest");

        // ✅ Handle Group Class Note student name default
        String noteType = sessionRequest.getSubject();
        if ("Overall".equals(noteType) || "Group".equals(noteType)) {
            String studentName = sessionRequest.getStudentName();
            if (studentName == null || studentName.trim().isEmpty()) {
                // Build name from group label or default to "Group"
                String groupLabel = sessionRequest.getGroupLabel();
                String numStudents = sessionRequest.getNumberOfStudents();

                if (groupLabel != null && !groupLabel.trim().isEmpty()) {
                    sessionRequest.setStudentName(groupLabel.trim());
                } else if (numStudents != null && !numStudents.trim().isEmpty()) {
                    sessionRequest.setStudentName("Group of " + numStudents.trim() + " students");
                } else {
                    sessionRequest.setStudentName("Group");
                }
            }
        }

        // ✅ MERGE: Combine engagementNotes + skillsNotes into keyPointers
        String engagementNotes = sessionRequest.getEngagementNotes();
        String skillsNotes     = sessionRequest.getSkillsNotes();

        StringBuilder mergedPointers = new StringBuilder();

        if (engagementNotes != null && !engagementNotes.trim().isEmpty()) {
            mergedPointers.append("Engagement & Behaviour: ").append(engagementNotes.trim());
        }
        if (skillsNotes != null && !skillsNotes.trim().isEmpty()) {
            if (mergedPointers.length() > 0) mergedPointers.append("\n\n");
            mergedPointers.append("Skills & Specific Moments: ").append(skillsNotes.trim());
        }

        sessionRequest.setKeyPointers(mergedPointers.toString());

        // ✅ SPARSE INPUT CHECK: Block generation if observations are too brief
        String engTrimmed   = engagementNotes != null ? engagementNotes.trim() : "";
        String skillsTrimmed = skillsNotes    != null ? skillsNotes.trim()     : "";
        String combined     = (engTrimmed + " " + skillsTrimmed).trim();
        int wordCount       = combined.isEmpty() ? 0 : combined.split("\\s+").length;

        if (wordCount < 8) {
            session.setAttribute("lastSessionRequest", sessionRequest);
            return "redirect:/notes?error=sparse";
        }

//        System.out.println("=== GENERATE RECEIVED ===");
//        System.out.println("Student:    " + sessionRequest.getStudentName());
//        System.out.println("Engagement: " + sessionRequest.getEngagementNotes());
//        System.out.println("Skills:     " + sessionRequest.getSkillsNotes());
//        System.out.println("Merged:     " + sessionRequest.getKeyPointers());
//        System.out.println("Standard:   " + sessionRequest.getCurriculumStandard());
//        System.out.println("=========================");

        log.debug("=== GENERATE RECEIVED ===");
        log.debug("Student:    {}", sessionRequest.getStudentName());
        log.debug("Engagement: {}", sessionRequest.getEngagementNotes());
        log.debug("Skills:     {}", sessionRequest.getSkillsNotes());
        log.debug("Merged:     {}", sessionRequest.getKeyPointers());
        log.debug("Standard:   {}", sessionRequest.getCurriculumStandard());

        String result = openAiService.generateSessionNote(sessionRequest);

        session.setAttribute("generatedNote", result);
        session.setAttribute("lastSessionRequest", sessionRequest);

        return "redirect:/notes";
    }

    // ✅ POST: Save approved note to DB
    @PostMapping("/approve")
    public String approveAndSaveNote(
            @RequestParam("finalApprovedNote")                    String finalApprovedNote,
            @RequestParam(value = "noteType",        defaultValue = "Student") String noteType,
            @RequestParam(value = "keyPointers",     defaultValue = "") String keyPointers,
            @RequestParam(value = "studentName",     defaultValue = "Group") String studentName,
            @RequestParam(value = "gradeLevel",      defaultValue = "") String gradeLevel,
            @RequestParam(value = "engagement",      defaultValue = "") String engagement,
            @RequestParam(value = "tutorName",       defaultValue = "") String tutorName,
            @RequestParam(value = "sessionDate",     defaultValue = "") String sessionDate,
            @RequestParam(value = "districtOrState", defaultValue = "") String districtOrState,
            @RequestParam(value = "curriculumStandard", defaultValue = "") String curriculumStandard,
            Model model) {

        // ✅ Default student name to "Group" if empty
        if (studentName == null || studentName.trim().isEmpty()) {
            studentName = "Group";
        }

        SessionNote note = new SessionNote();
        note.setStudentName(studentName);
        note.setSubject(noteType);
        note.setGradeLevel(gradeLevel);
        note.setEngagement(engagement);
        note.setRawPointers(keyPointers);
        note.setFinalApprovedNote(finalApprovedNote);
        note.setTutorName(tutorName);
        note.setSessionDate(sessionDate);
        note.setDistrictOrState(districtOrState);
        note.setCurriculumStandard(curriculumStandard);

        noteRepository.save(note);
        return "redirect:/notes?success=true";
    }
}