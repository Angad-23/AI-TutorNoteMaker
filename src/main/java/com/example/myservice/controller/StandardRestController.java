package com.example.myservice.controller;

import com.example.myservice.entity.MathStandard;
import com.example.myservice.repository.MathStandardRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/standards")
public class StandardRestController {

    private final MathStandardRepository standardRepository;

    public StandardRestController(MathStandardRepository standardRepository) {
        this.standardRepository = standardRepository;
    }

    // Get grades available for a selected state
    @GetMapping("/grades")
    public List<String> getGrades(@RequestParam String state) {
        return standardRepository.findDistinctGradesByState(state);
    }

    // Get codes and full descriptions for a combined selection match
    @GetMapping("/list")
    public List<MathStandard> getStandards(@RequestParam String state, @RequestParam String grade) {
        return standardRepository.findByStateAndGrade(state, grade);
    }
}