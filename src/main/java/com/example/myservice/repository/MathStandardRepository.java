package com.example.myservice.repository;

import com.example.myservice.entity.MathStandard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface MathStandardRepository extends JpaRepository<MathStandard, Long> {

    @Query("SELECT DISTINCT m.state FROM MathStandard m")
    List<String> findDistinctStates();

    @Query("SELECT DISTINCT m.grade FROM MathStandard m WHERE m.state = :state")
    List<String> findDistinctGradesByState(String state);

    List<MathStandard> findByStateAndGrade(String state, String grade);
}