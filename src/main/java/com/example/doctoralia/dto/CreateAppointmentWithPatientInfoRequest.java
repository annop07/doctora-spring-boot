package com.example.doctoralia.dto;

// Jakarta validation will be handled in service layer

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CreateAppointmentWithPatientInfoRequest {
    // Appointment basic info
    private Long doctorId;
    private LocalDateTime appointmentDateTime;
    private Integer durationMinutes = 30;
    private String notes;

    // Patient detailed info
    private String patientPrefix;
    private String patientFirstName;
    private String patientLastName;
    private String patientGender;
    private LocalDate patientDateOfBirth;
    private String patientNationality;
    private String patientCitizenId;
    private String patientPhone;
    private String patientEmail;

    // Additional booking info
    private String symptoms;
    private String bookingType;
    private String queueNumber;

    // Constructors
    public CreateAppointmentWithPatientInfoRequest() {}

    // Getters and Setters
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public LocalDateTime getAppointmentDateTime() { return appointmentDateTime; }
    public void setAppointmentDateTime(LocalDateTime appointmentDateTime) {
        this.appointmentDateTime = appointmentDateTime;
    }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    // Patient info getters/setters
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
    public void setPatientNationality(String patientNationality) {
        this.patientNationality = patientNationality;
    }

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

    @Override
    public String toString() {
        return "CreateAppointmentWithPatientInfoRequest{" +
                "doctorId=" + doctorId +
                ", appointmentDateTime=" + appointmentDateTime +
                ", patientFirstName='" + patientFirstName + '\'' +
                ", patientLastName='" + patientLastName + '\'' +
                ", patientEmail='" + patientEmail + '\'' +
                ", bookingType='" + bookingType + '\'' +
                '}';
    }
}