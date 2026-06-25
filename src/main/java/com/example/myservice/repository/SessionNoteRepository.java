package com.example.myservice.repository;

import com.example.myservice.entity.SessionNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionNoteRepository extends JpaRepository<SessionNote, Long> {

        // Secure the 5-item preview table for the logged-in tutor
        List<SessionNote> findTop5ByTutorNameOrderByCreatedAtDesc(String tutorName);

        // Secure the complete historical record list for the logged-in tutor
        List<SessionNote> findAllByTutorNameOrderByCreatedAtDesc(String tutorName);
}