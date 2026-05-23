package com.example.myservice.repository;

import com.example.myservice.entity.SessionNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionNoteRepository extends JpaRepository<SessionNote, Long> {
    // Just extending JpaRepository automatically gives us .save() and .findAll()!

        // Returns only the 5 most recent entries
        List<SessionNote> findTop5ByOrderByCreatedAtDesc();

}