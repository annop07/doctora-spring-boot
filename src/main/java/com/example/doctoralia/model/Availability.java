package com.example.doctoralia.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "availabilities")
public class Availability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Many-to-One relationship with Doctor
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doctor_id",nullable = false)
    private Doctor doctor;

    @Column(name = "day_of_week",nullable = false)
    @Min(value = 1, message = "Day of week must be between 1-7")
    @Max(value = 7, message = "Day of week must be between 1-7")
    private Integer dayOfWeek; // 1=Monday, 2=Tuesday, ..., 7=Sunday

    @Column(name = "start_time",nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time",nullable = false)
    private LocalTime endTime;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public Availability() {}

    public Availability(Doctor doctor, Integer dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.doctor = doctor;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isActive = true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper methods
    public String getDayName() {
        String[] days = {"", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        return dayOfWeek >= 1 && dayOfWeek <= 7 ? days[dayOfWeek] : "Unknown";
    }

    public String getTimeRange() {
        return startTime + " - " + endTime;
    }

    @Override
    public String toString() {
        return "Availability{" +
                "id=" + id +
                ", doctorName='" + (doctor != null ? doctor.getDoctorName() : "N/A") + '\'' +
                ", dayOfWeek=" + getDayName() +
                ", timeRange='" + getTimeRange() + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
