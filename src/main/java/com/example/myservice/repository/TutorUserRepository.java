package com.example.myservice.repository;

import com.example.myservice.entity.TutorUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TutorUserRepository extends JpaRepository<TutorUser, Long> {
    Optional<TutorUser> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}