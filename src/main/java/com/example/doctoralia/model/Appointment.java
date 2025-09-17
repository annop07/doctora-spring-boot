package com.example.doctoralia.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    //Many-to-One relationship with Doctor
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    //Many-to-One relationship with Patient (User)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @Column(name = "appointment_datetime", nullable = false)
    private LocalDateTime appointmentDateTime;

    @Column(name = "duration_minutes")
    @Min(value = 15,message = "Duration must be at least 15 minutes")
    private Integer durationMinutes = 30; // Default 30 minutes

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status = AppointmentStatus.PENDING;

    private String notes; //หมายเหตุจากคนไข้

    @Column(name = "doctor_notes")
    private String doctorNotes; //หมายเหตุจากหมอ

    @CreationTimestamp
    @Column(name = "created_at",updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public Appointment() {}

    public Appointment(Doctor doctor, User patient, LocalDateTime appointmentDateTime) {
        this.doctor = doctor;
        this.patient = patient;
        this.appointmentDateTime = appointmentDateTime;
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

    public User getPatient() {
        return patient;
    }

    public void setPatient(User patient) {
        this.patient = patient;
    }

    public LocalDateTime getAppointmentDateTime() {
        return appointmentDateTime;
    }

    public void setAppointmentDateTime(LocalDateTime appointmentDateTime) {
        this.appointmentDateTime = appointmentDateTime;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getDoctorNotes() {
        return doctorNotes;
    }

    public void setDoctorNotes(String doctorNotes) {
        this.doctorNotes = doctorNotes;
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
    public LocalDateTime getAppointmentEndTime() {
        return appointmentDateTime.plusMinutes(durationMinutes);
    }

    public String getDoctorName() {
        return doctor != null ? doctor.getDoctorName() : "";
    }

    public String getPatientName() {
        return patient != null ? patient.getFullName() : "";
    }

    public boolean isInThePast() {
        return appointmentDateTime.isBefore(LocalDateTime.now());
    }

    public boolean canBeCancelled() {
        return status == AppointmentStatus.PENDING || status == AppointmentStatus.CONFIRMED;
    }

    @Override
    public String toString() {
        return "Appointment{" +
                "id=" + id +
                ", doctorName='" + getDoctorName() + '\'' +
                ", patientName='" + getPatientName() + '\'' +
                ", appointmentDateTime=" + appointmentDateTime +
                ", status=" + status +
                '}';
    }
}
