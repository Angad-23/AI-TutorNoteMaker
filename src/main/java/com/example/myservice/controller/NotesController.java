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

    // 🌟 GET: ONLY displays the page and reads from the DB. No saving allowed here!
    @GetMapping
    public String showForm(@AuthenticationPrincipal UserDetails userDetails,
                           @RequestParam(value = "success", required = false) String success,
                           Model model) {
        SessionRequest sessionRequest = new SessionRequest();

        // ✅ Auto-fill tutor name from logged-in account
        if (userDetails != null) {
            tutorUserRepository.findByUsername(userDetails.getUsername())
                    .ifPresent(tutor -> sessionRequest.setTutorName(tutor.getFullName()));
        }

        model.addAttribute("sessionRequest", sessionRequest);
        model.addAttribute("savedNotes", noteRepository.findTop5ByOrderByCreatedAtDesc());
        if ("true".equals(success)) model.addAttribute("saveSuccess", true);
        return "tutor-form";
    }

    // 🌟 POST: Generates the initial draft text from Groq AI
    @PostMapping("/generate")
    public String generateNote(@ModelAttribute SessionRequest sessionRequest, Model model) {
        String result = openAiService.generateSessionNote(sessionRequest);
        model.addAttribute("generatedNote", result);
        model.addAttribute("sessionRequest", sessionRequest);
        model.addAttribute("savedNotes", noteRepository.findTop5ByOrderByCreatedAtDesc()
        ); // Keeps table visible
        return "tutor-form";
    }

    // 🌟 POST: The ONLY place where data is written to the database
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

        // This line runs EXCLUSIVELY when the green button is clicked
        noteRepository.save(note);

        // 🌟 Post-Redirect-Get pattern workaround for demo:
        // We pass the success flag and clear the input form
        return "redirect:/notes?success=true";
    }
}