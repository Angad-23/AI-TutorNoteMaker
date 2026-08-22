package com.example.myservice.service;

import com.example.myservice.entity.SessionNote;
import com.example.myservice.entity.TutorUser;
import com.example.myservice.repository.SessionNoteRepository;
import com.example.myservice.repository.TutorUserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final SessionNoteRepository noteRepository;
    private final TutorUserRepository tutorUserRepository;

    public DashboardService(SessionNoteRepository noteRepository,
                            TutorUserRepository tutorUserRepository) {
        this.noteRepository = noteRepository;
        this.tutorUserRepository = tutorUserRepository;
    }

    public TutorUser getTutor(String username) {
        return tutorUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Tutor not found"));
    }

    // Same matching rule as the original dashboard: full name OR username, case-insensitive
    public List<SessionNote> getAllNotesForTutor(TutorUser tutor) {
        return noteRepository.findAll().stream()
                .filter(n -> n.getTutorName() != null && (
                        n.getTutorName().equalsIgnoreCase(tutor.getFullName()) ||
                                n.getTutorName().equalsIgnoreCase(tutor.getUsername())
                ))
                .collect(Collectors.toList());
    }

    public String todayString() {
        return LocalDate.now(ZoneId.of("Asia/Kolkata")).toString();
    }

    public List<SessionNote> getTodayNotes(List<SessionNote> allNotes) {
        String today = todayString();
        return allNotes.stream()
                .filter(n -> n.getSessionDate() != null && n.getSessionDate().equals(today))
                .collect(Collectors.toList());
    }

    public List<SessionNote> getRecentNotes(List<SessionNote> allNotes, int limit) {
        return allNotes.stream()
                .filter(n -> n.getCreatedAt() != null)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(limit)
                .collect(Collectors.toList());
    }
}