package com.example.myservice.controller;

import com.example.myservice.entity.SessionNote;
import com.example.myservice.entity.TutorUser;
import com.example.myservice.repository.SessionNoteRepository;
import com.example.myservice.repository.TutorUserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    private final SessionNoteRepository noteRepository;
    private final TutorUserRepository tutorUserRepository;

    public DashboardController(SessionNoteRepository noteRepository,
                               TutorUserRepository tutorUserRepository) {
        this.noteRepository = noteRepository;
        this.tutorUserRepository = tutorUserRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {

        String username = userDetails.getUsername();

        // Get tutor full name from DB
        TutorUser tutor = tutorUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Tutor not found"));

        // ✅ Match by BOTH full name AND username — case insensitive
        List<SessionNote> allNotes = noteRepository.findAll().stream()
                .filter(n -> n.getTutorName() != null && (
                        n.getTutorName().equalsIgnoreCase(tutor.getFullName()) ||
                                n.getTutorName().equalsIgnoreCase(tutor.getUsername())
                ))
                .collect(Collectors.toList());

        // Notes saved today
//        String today = LocalDate.now().toString(); // 2026-05-25
        String today = LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")).toString();

        System.out.println("TODAY IS: " + today);
        System.out.println("SESSION DATES IN DB: " + allNotes.stream()
                .map(SessionNote::getSessionDate)
                .collect(Collectors.toList()));

// ✅ Notes today
        long todayCount = allNotes.stream()
                .filter(n -> n.getSessionDate() != null &&
                        n.getSessionDate().equals(today))
                .count();

// ✅ DEBUG - put AFTER todayCount
        System.out.println("TODAY IS: " + today);
        System.out.println("ALL NOTES COUNT: " + allNotes.size());
        System.out.println("TUTOR FULL NAME: " + tutor.getFullName());
        System.out.println("TODAY COUNT: " + todayCount);
        System.out.println("SESSION DATES: " + allNotes.stream()
                .map(SessionNote::getSessionDate)
                .collect(Collectors.toList()));

        // Unique students taught
        long uniqueStudents = allNotes.stream()
                .map(SessionNote::getStudentName)
                .filter(s -> s != null && !s.isEmpty())
                .distinct()
                .count();

        // Last 5 notes sorted by created date
        List<SessionNote> recentNotes = allNotes.stream()
                .filter(n -> n.getCreatedAt() != null)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .collect(Collectors.toList());

        model.addAttribute("username", username);
        model.addAttribute("fullName", tutor.getFullName());
        model.addAttribute("totalNotes", allNotes.size());
        model.addAttribute("todayNotes", todayCount);
        model.addAttribute("totalStudents", uniqueStudents);
        model.addAttribute("recentNotes", recentNotes);

        return "dashboard";
    }
}