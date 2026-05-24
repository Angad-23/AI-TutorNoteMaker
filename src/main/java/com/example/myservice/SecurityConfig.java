package com.example.myservice;

import com.example.myservice.repository.TutorUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final TutorUserRepository tutorUserRepository;

    public SecurityConfig(TutorUserRepository tutorUserRepository) {
        this.tutorUserRepository = tutorUserRepository;
    }

    // ✅ Controls which pages need login and which are public
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login",
                                "/register",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/*.png",      // ✅ Allow all PNG files
                                "/*.jpg",      // ✅ Allow all JPG files
                                "/*.ico",      // ✅ Allow favicon
                                "/static/**"   // ✅ Allow static folder
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll()
                );
        return http.build();
    }

    // ✅ Loads tutor from DB by username for authentication
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> tutorUserRepository.findByUsername(username)
                .map(tutor -> User.builder()
                        .username(tutor.getUsername())
                        .password(tutor.getPassword())
                        .roles(tutor.getRole())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    // ✅ BCrypt password encoder — never stores plain text passwords
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}