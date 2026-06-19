package com.example.myservice.controller;

import com.example.myservice.dto.SessionRequest;
import com.example.myservice.entity.SessionNote;
import com.example.myservice.repository.SessionNoteRepository;
import com.example.myservice.service.OpenAiService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import com.example.myservice.repository.TutorUserRepository;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @GetMapping
    public String showForm(@AuthenticationPrincipal UserDetails userDetails,
                           @RequestParam(value = "success", required = false) String success,
                           Model model,
                           HttpSession session) {

        // ✅ Read from HTTP session
        String generatedNote = (String) session.getAttribute("generatedNote");
        SessionRequest sessionRequest = (SessionRequest) session.getAttribute("lastSessionRequest");

        System.out.println(">>> [SESSION CHECK] generatedNote = " + generatedNote);
        System.out.println(">>> [SESSION CHECK] sessionRequest = " + sessionRequest);

        if (sessionRequest == null) {
            sessionRequest = new SessionRequest();
        }

        String currentTutor = getLoggedInTutorName(userDetails);
        sessionRequest.setTutorName(currentTutor);

        if (generatedNote != null) {
            model.addAttribute("generatedNote", generatedNote);
            // ✅ Clear after use so it doesn't re-show on refresh
            session.removeAttribute("generatedNote");
            session.removeAttribute("lastSessionRequest");
        }

        model.addAttribute("sessionRequest", sessionRequest);
        model.addAttribute("savedNotes",
                noteRepository.findTop5ByTutorNameOrderByCreatedAtDesc(currentTutor));

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
    // ✅ FIXED: now follows the POST-Redirect-GET pattern instead of returning
    // the view directly. This prevents the browser from caching/resubmitting
    // a stale POST (with an outdated CSRF token) when the user reloads the page,
    // which was previously causing 403 Forbidden errors until a hard refresh.
    @PostMapping("/generate")
    public String generateNote(@AuthenticationPrincipal UserDetails userDetails,
                               @ModelAttribute SessionRequest sessionRequest,
                               HttpSession session) {

        String result = openAiService.generateSessionNote(sessionRequest);

        // ✅ Store in HTTP session — NOT flash attributes
        session.setAttribute("generatedNote", result);
        session.setAttribute("lastSessionRequest", sessionRequest);

        return "redirect:/notes";
    }

@PostMapping("/approve")
public String approveAndSaveNote(
        @RequestParam("finalApprovedNote") String finalApprovedNote,
        @RequestParam(value = "noteType", defaultValue = "Student") String noteType,  // ✅ was "subject"
        @RequestParam(value = "keyPointers",     defaultValue = "") String keyPointers,
        @RequestParam(value = "studentName",     defaultValue = "") String studentName,
        @RequestParam(value = "gradeLevel",      defaultValue = "") String gradeLevel,
        @RequestParam(value = "engagement",      defaultValue = "") String engagement,
        @RequestParam(value = "tutorName",       defaultValue = "") String tutorName,
        @RequestParam(value = "sessionDate",     defaultValue = "") String sessionDate,
        @RequestParam(value = "districtOrState", defaultValue = "") String districtOrState,
        @RequestParam(value = "curriculumStandard",  defaultValue = "") String curriculumStandard,
        Model model) {

    SessionNote note = new SessionNote();
    note.setStudentName(studentName);
    note.setSubject(noteType);          // ✅ maps to note_type column
    note.setGradeLevel(gradeLevel);
    note.setEngagement(engagement);
    note.setRawPointers(keyPointers);
    note.setFinalApprovedNote(finalApprovedNote);
    note.setTutorName(tutorName);
    note.setSessionDate(sessionDate);
    note.setDistrictOrState(districtOrState);
    note.setCurriculumStandard(curriculumStandard); // ✅ NEW


    noteRepository.save(note);
    return "redirect:/notes?success=true";
}
}