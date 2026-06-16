package com.example.myservice.service;

import com.example.myservice.entity.MathStandard;
import com.example.myservice.repository.MathStandardRepository;
import com.opencsv.CSVReader;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
public class DataInitializer implements CommandLineRunner {

    private final MathStandardRepository standardRepository;

    public DataInitializer(MathStandardRepository standardRepository) {
        this.standardRepository = standardRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Only seed the database if it doesn't have records yet
        if (standardRepository.count() == 0) {
            System.out.println(">>> Math standards database table is empty. Initializing data loading process...");

            ClassPathResource resource = new ClassPathResource("Math_Standards.csv");

            try (CSVReader csvReader = new CSVReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String[] values;

                // Skip the header row (State, Grade, Code, Description)
                csvReader.readNext();

                int recordCount = 0;
                while ((values = csvReader.readNext()) != null) {
                    // Safety check to handle potential trailing blank lines
                    if (values.length < 4) continue;

                    MathStandard standard = new MathStandard();
                    standard.setState(values[0].trim());
                    standard.setGrade(values[1].trim());
                    standard.setCode(values[2].trim());
                    standard.setDescription(values[3].trim());

                    standardRepository.save(standard);
                    recordCount++;
                }
                System.out.println(">>> Successfully imported " + recordCount + " math curriculum standards into MySQL database!");
            } catch (Exception e) {
                System.err.println(">>> Failed to import math curriculum standards: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println(">>> Math standards table already populated. Skipping database seeding initialization step.");
        }
    }
}