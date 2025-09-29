package com.example.doctoralia.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "patient_booking_info")
public class PatientBookingInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    // Patient details at time of booking
    @Column(name = "patient_prefix", length = 20)
    private String patientPrefix;

    @Column(name = "patient_first_name", length = 100)
    private String patientFirstName;

    @Column(name = "patient_last_name", length = 100)
    private String patientLastName;

    @Column(name = "patient_gender", length = 20)
    private String patientGender;

    @Column(name = "patient_date_of_birth")
    private LocalDate patientDateOfBirth;

    @Column(name = "patient_nationality", length = 50)
    private String patientNationality;

    @Column(name = "patient_citizen_id", length = 13)
    private String patientCitizenId;

    @Column(name = "patient_phone", length = 20)
    private String patientPhone;

    @Column(name = "patient_email")
    private String patientEmail;

    // Additional booking info
    @Column(columnDefinition = "TEXT")
    private String symptoms;

    @Column(name = "booking_type", length = 20)
    private String bookingType; // 'auto' or 'manual'

    @Column(name = "queue_number", length = 10)
    private String queueNumber;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public PatientBookingInfo() {}

    public PatientBookingInfo(Appointment appointment, String patientPrefix, String patientFirstName,
                             String patientLastName, String patientGender, LocalDate patientDateOfBirth,
                             String patientNationality, String patientCitizenId, String patientPhone,
                             String patientEmail, String symptoms, String bookingType, String queueNumber) {
        this.appointment = appointment;
        this.patientPrefix = patientPrefix;
        this.patientFirstName = patientFirstName;
        this.patientLastName = patientLastName;
        this.patientGender = patientGender;
        this.patientDateOfBirth = patientDateOfBirth;
        this.patientNationality = patientNationality;
        this.patientCitizenId = patientCitizenId;
        this.patientPhone = patientPhone;
        this.patientEmail = patientEmail;
        this.symptoms = symptoms;
        this.bookingType = bookingType;
        this.queueNumber = queueNumber;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Appointment getAppointment() { return appointment; }
    public void setAppointment(Appointment appointment) { this.appointment = appointment; }

    public String getPatientPrefix() { return patientPrefix; }
    public void setPatientPrefix(String patientPrefix) { this.patientPrefix = patientPrefix; }

    public String getPatientFirstName() { return patientFirstName; }
    public void setPatientFirstName(String patientFirstName) { this.patientFirstName = patientFirstName; }

    public String getPatientLastName() { return patientLastName; }
    public void setPatientLastName(String patientLastName) { this.patientLastName = patientLastName; }

    public String getPatientGender() { return patientGender; }
    public void setPatientGender(String patientGender) { this.patientGender = patientGender; }

    public LocalDate getPatientDateOfBirth() { return patientDateOfBirth; }
    public void setPatientDateOfBirth(LocalDate patientDateOfBirth) {
        this.patientDateOfBirth = patientDateOfBirth;
    }

    public String getPatientNationality() { return patientNationality; }
    public void setPatientNationality(String patientNationality) { this.patientNationality = patientNationality; }

    public String getPatientCitizenId() { return patientCitizenId; }
    public void setPatientCitizenId(String patientCitizenId) { this.patientCitizenId = patientCitizenId; }

    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }

    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

    public String getBookingType() { return bookingType; }
    public void setBookingType(String bookingType) { this.bookingType = bookingType; }

    public String getQueueNumber() { return queueNumber; }
    public void setQueueNumber(String queueNumber) { this.queueNumber = queueNumber; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Helper methods
    public String getPatientFullName() {
        StringBuilder fullName = new StringBuilder();
        if (patientPrefix != null && !patientPrefix.trim().isEmpty()) {
            fullName.append(patientPrefix).append(" ");
        }
        if (patientFirstName != null && !patientFirstName.trim().isEmpty()) {
            fullName.append(patientFirstName).append(" ");
        }
        if (patientLastName != null && !patientLastName.trim().isEmpty()) {
            fullName.append(patientLastName);
        }
        return fullName.toString().trim();
    }

    @Override
    public String toString() {
        return "PatientBookingInfo{" +
                "id=" + id +
                ", appointmentId=" + (appointment != null ? appointment.getId() : null) +
                ", patientFullName='" + getPatientFullName() + '\'' +
                ", patientEmail='" + patientEmail + '\'' +
                ", bookingType='" + bookingType + '\'' +
                ", queueNumber='" + queueNumber + '\'' +
                '}';
    }
}