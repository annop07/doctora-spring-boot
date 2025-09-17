package com.example.doctoralia.dto;

import jakarta.validation.constraints.NotBlank;

public class RejectAppointmentRequest {

    @NotBlank(message = "Reason is required")
    private String reason;

    // Constructors
    public RejectAppointmentRequest() {}

    public RejectAppointmentRequest(String reason) {
        this.reason = reason;
    }

    // Getters and Setters
    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}