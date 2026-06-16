package com.example.myservice.controller;

import com.example.myservice.dto.SessionRequest;
import com.example.myservice.entity.SessionNote;
import com.example.myservice.repository.SessionNoteRepository;
import com.example.myservice.service.OpenAiService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import com.example.myservice.repository.TutorUserRepository;
import java.util.List;

@Controller
@RequestMapping("/notes")
public class NotesController {

    private final OpenAiService openAiService;
    private final SessionNoteRepository noteRepository;
    private final TutorUserRepository tutorUserRepository;

    public NotesController(OpenAiService openAiService,
                           SessionNoteRepository noteRepository,
                           TutorUserRepository tutorUserRepository) {
        this.openAiService = openAiService;
        this.noteRepository = noteRepository;
        this.tutorUserRepository = tutorUserRepository;
    }

    // Helper helper strategy to extract the full profile name of the logged-in user securely
    private String getLoggedInTutorName(UserDetails userDetails) {
        if (userDetails != null) {
            return tutorUserRepository.findByUsername(userDetails.getUsername())
                    .map(tutor -> tutor.getFullName())
                    .orElse("Unknown Tutor");
        }
        return "Unknown Tutor";
    }

    // 🌟 GET: Main Workplace Dashboard Form
    @GetMapping
    public String showForm(@AuthenticationPrincipal UserDetails userDetails,
                           @RequestParam(value = "success", required = false) String success,
                           Model model) {
        SessionRequest sessionRequest = new SessionRequest();
        String currentTutor = getLoggedInTutorName(userDetails);

        sessionRequest.setTutorName(currentTutor);

        model.addAttribute("sessionRequest", sessionRequest);
        // ✅ SECURED: Only displays this specific tutor's top 5 recent notes
        model.addAttribute("savedNotes", noteRepository.findTop5ByTutorNameOrderByCreatedAtDesc(currentTutor));

        if ("true".equals(success)) model.addAttribute("saveSuccess", true);
        return "tutor-form";
    }

    // 🌟 GET: Clean, Sorted User History View
    @GetMapping("/history")
    public String viewNotesHistory(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        System.out.println(">>> [DEBUG] /notes/history endpoint was successfully hit!");

        String currentTutor = getLoggedInTutorName(userDetails);
        System.out.println(">>> [DEBUG] Fetching exclusive history records for: " + currentTutor);

        // ✅ SECURED: Using the isolated user lookup query method
        List<SessionNote> savedNotes = noteRepository.findAllByTutorNameOrderByCreatedAtDesc(currentTutor);

        System.out.println(">>> [DEBUG] Total records retrieved from MySQL: " + (savedNotes != null ? savedNotes.size() : 0));

        model.addAttribute("savedNotes", savedNotes);
        return "notes-history";
    }

    // 🌟 POST: Generates the initial draft text from Groq AI
    @PostMapping("/generate")
    public String generateNote(@AuthenticationPrincipal UserDetails userDetails,
                               @ModelAttribute SessionRequest sessionRequest,
                               Model model) {
        String result = openAiService.generateSessionNote(sessionRequest);
        String currentTutor = getLoggedInTutorName(userDetails);

        model.addAttribute("generatedNote", result);
        model.addAttribute("sessionRequest", sessionRequest);
        // ✅ SECURED: Keeps recent items matching the login profile
        model.addAttribute("savedNotes", noteRepository.findTop5ByTutorNameOrderByCreatedAtDesc(currentTutor));
        return "tutor-form";
    }

    // 🌟 POST: Saves details securely to your MySQL engine backend
    @PostMapping("/approve")
    public String approveAndSaveNote(
            @RequestParam("finalApprovedNote") String finalApprovedNote,
            @RequestParam("subject") String subject,
            @RequestParam("keyPointers") String keyPointers,
            @RequestParam("studentName") String studentName,
            @RequestParam("gradeLevel") String gradeLevel,
            @RequestParam("engagement") String engagement,
            @RequestParam("tutorName") String tutorName,
            @RequestParam("sessionDate") String sessionDate,
            @RequestParam("districtOrState") String districtOrState,
            @RequestParam(value = "noteType", defaultValue = "Single") String noteType,
            Model model) {

        SessionNote note = new SessionNote();
        note.setStudentName(studentName);
        note.setSubject(subject);
        note.setGradeLevel(gradeLevel);
        note.setEngagement(engagement);
        note.setRawPointers(keyPointers);
        note.setFinalApprovedNote(finalApprovedNote);
        note.setTutorName(tutorName);
        note.setSessionDate(sessionDate);
        note.setDistrictOrState(districtOrState);
//        note.setNoteType(noteType);

        noteRepository.save(note);

        return "redirect:/notes?success=true";
    }
}