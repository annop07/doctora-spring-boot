package com.example.doctoralia.dto;

import com.example.doctoralia.model.Doctor;
import com.example.doctoralia.model.Specialty;

public class SpecialtyWithDoctorCount {
    private Specialty specialty;
    private long doctorCount;

    public SpecialtyWithDoctorCount(Specialty specialty, long doctorCount) {
        this.specialty = specialty;
        this.doctorCount = doctorCount;
    }

    // Getters
    public Specialty getSpecialty() { return specialty; }
    public long getDoctorCount() { return doctorCount; }
}
