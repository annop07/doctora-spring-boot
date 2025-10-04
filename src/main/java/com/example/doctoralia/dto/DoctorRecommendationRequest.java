package com.example.doctoralia.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public class DoctorRecommendationRequest {

    private Long specialtyId;

    private String symptoms;

    @DecimalMin(value = "0.0", message = "ค่าบริการสูงสุดต้องไม่ติดลบ")
    private BigDecimal maxFee;

    @Min(value = 0, message = "ประสบการณ์สูงสุดต้องไม่ติดลบ")
    @Max(value = 50, message = "ประสบการณ์สูงสุดไม่ควรเกิน 50 ปี")
    private Integer maxExperienceYears;

    private String preferredGender; // "male", "female", null

    @Min(value = 1, message = "คะแนนขั้นต่ำต้องอยู่ระหว่าง 1-5")
    @Max(value = 5, message = "คะแนนขั้นต่ำต้องอยู่ระหว่าง 1-5")
    private Integer minRating;

    private String preferredTimeSlot; // "morning", "afternoon", "evening"

    private Boolean urgentCase = false;

    // Constructors
    public DoctorRecommendationRequest() {}

    public DoctorRecommendationRequest(Long specialtyId, String symptoms) {
        this.specialtyId = specialtyId;
        this.symptoms = symptoms;
    }

    // Getters and Setters
    public Long getSpecialtyId() {
        return specialtyId;
    }

    public void setSpecialtyId(Long specialtyId) {
        this.specialtyId = specialtyId;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public BigDecimal getMaxFee() {
        return maxFee;
    }

    public void setMaxFee(BigDecimal maxFee) {
        this.maxFee = maxFee;
    }

    public Integer getMaxExperienceYears() {
        return maxExperienceYears;
    }

    public void setMaxExperienceYears(Integer maxExperienceYears) {
        this.maxExperienceYears = maxExperienceYears;
    }

    public String getPreferredGender() {
        return preferredGender;
    }

    public void setPreferredGender(String preferredGender) {
        this.preferredGender = preferredGender;
    }

    public Integer getMinRating() {
        return minRating;
    }

    public void setMinRating(Integer minRating) {
        this.minRating = minRating;
    }

    public String getPreferredTimeSlot() {
        return preferredTimeSlot;
    }

    public void setPreferredTimeSlot(String preferredTimeSlot) {
        this.preferredTimeSlot = preferredTimeSlot;
    }

    public Boolean getUrgentCase() {
        return urgentCase;
    }

    public void setUrgentCase(Boolean urgentCase) {
        this.urgentCase = urgentCase;
    }

    @Override
    public String toString() {
        return "DoctorRecommendationRequest{" +
                "specialtyId=" + specialtyId +
                ", symptoms='" + symptoms + '\'' +
                ", maxFee=" + maxFee +
                ", maxExperienceYears=" + maxExperienceYears +
                ", preferredGender='" + preferredGender + '\'' +
                ", minRating=" + minRating +
                ", preferredTimeSlot='" + preferredTimeSlot + '\'' +
                ", urgentCase=" + urgentCase +
                '}';
    }
}