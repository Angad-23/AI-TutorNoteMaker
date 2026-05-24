package com.example.myservice.controller;

import com.example.myservice.entity.TutorUser;
import com.example.myservice.repository.TutorUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final TutorUserRepository tutorUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(TutorUserRepository tutorUserRepository,
                          PasswordEncoder passwordEncoder) {
        this.tutorUserRepository = tutorUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ✅ Show login page
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        if (error != null)  model.addAttribute("errorMsg", "Invalid username or password.");
        if (logout != null) model.addAttribute("logoutMsg", "You have been logged out successfully.");

        return "login";
    }

    // ✅ Show register page
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("tutorUser", new TutorUser());
        return "register";
    }

    // ✅ Handle registration form submission
    @PostMapping("/register")
    public String registerTutor(@ModelAttribute TutorUser tutorUser,
                                @RequestParam String confirmPassword,
                                Model model) {

        // Check if username already exists
        if (tutorUserRepository.existsByUsername(tutorUser.getUsername())) {
            model.addAttribute("errorMsg", "Username already taken. Please choose another.");
            return "register";
        }

        // Check if email already exists
        if (tutorUserRepository.existsByEmail(tutorUser.getEmail())) {
            model.addAttribute("errorMsg", "Email already registered.");
            return "register";
        }

        // Check passwords match
        if (!tutorUser.getPassword().equals(confirmPassword)) {
            model.addAttribute("errorMsg", "Passwords do not match.");
            return "register";
        }

        // Encode password before saving — NEVER store plain text
        tutorUser.setPassword(passwordEncoder.encode(tutorUser.getPassword()));
        tutorUser.setRole("TUTOR");
        tutorUserRepository.save(tutorUser);

        return "redirect:/login?registered=true";
    }
}