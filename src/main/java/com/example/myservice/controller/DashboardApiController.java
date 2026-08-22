package com.example.myservice.controller;

import com.example.myservice.dto.NoteSummaryDTO;
import com.example.myservice.dto.StudentSummaryDTO;
import com.example.myservice.entity.SessionNote;
import com.example.myservice.entity.TutorUser;
import com.example.myservice.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {

    private final DashboardService dashboardService;

    public DashboardApiController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // GET /api/dashboard/notes?filter=all   -> every note for the logged-in tutor
    // GET /api/dashboard/notes?filter=today -> only today's notes
    @GetMapping("/notes")
    public List<NoteSummaryDTO> getNotes(@AuthenticationPrincipal UserDetails userDetails,
                                         @RequestParam(defaultValue = "all") String filter) {

        TutorUser tutor = dashboardService.getTutor(userDetails.getUsername());
        List<SessionNote> allNotes = dashboardService.getAllNotesForTutor(tutor);

        List<SessionNote> filtered = "today".equalsIgnoreCase(filter)
                ? dashboardService.getTodayNotes(allNotes)
                : allNotes;

        return filtered.stream()
                .sorted(Comparator.comparing(SessionNote::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(n -> new NoteSummaryDTO(
                        n.getStudentName(),
                        n.getSubject(),
                        n.getGradeLevel(),
                        n.getDistrictOrState(),
                        n.getEngagement(),
                        n.getSessionDate(),
                        n.getFinalApprovedNote()
                ))
                .collect(Collectors.toList());
    }

    // GET /api/dashboard/students -> one row per distinct student, sorted by most recent session first
    @GetMapping("/students")
    public List<StudentSummaryDTO> getStudents(@AuthenticationPrincipal UserDetails userDetails) {

        TutorUser tutor = dashboardService.getTutor(userDetails.getUsername());
        List<SessionNote> allNotes = dashboardService.getAllNotesForTutor(tutor);

        Map<String, List<SessionNote>> byStudent = allNotes.stream()
                .filter(n -> n.getStudentName() != null && !n.getStudentName().isEmpty())
                .collect(Collectors.groupingBy(SessionNote::getStudentName));

        return byStudent.entrySet().stream()
                .map(e -> {
                    String lastDate = e.getValue().stream()
                            .map(SessionNote::getSessionDate)
                            .filter(d -> d != null && !d.isEmpty())
                            .max(String::compareTo)
                            .orElse(null);
                    return new StudentSummaryDTO(e.getKey(), e.getValue().size(),
                            lastDate != null ? lastDate : "—");
                })
                // Sort by date string descending (latest first); students with no date sink to the bottom
                .sorted(Comparator.comparing(
                        (StudentSummaryDTO s) -> "—".equals(s.getLastSessionDate()) ? "" : s.getLastSessionDate(),
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }
}