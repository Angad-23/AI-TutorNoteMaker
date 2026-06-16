package com.example.myservice.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "math_standards")
@Data
public class MathStandard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String state;       // e.g., Florida, Maryland, Common Core
    private String grade;       // e.g., K, I, II, Algebra 1
    private String code;        // e.g., MA.K.NSO.1.1

    @Column(length = 2000)
    private String description; // The full text of the standard
}