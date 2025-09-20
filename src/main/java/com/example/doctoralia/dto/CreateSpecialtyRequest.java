package com.example.doctoralia.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateSpecialtyRequest {
    @NotBlank(message = "Specialty name is required")
    private String name;

    private String description;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
