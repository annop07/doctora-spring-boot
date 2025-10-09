package com.example.doctoralia.dto;

public class DoctorStats {
    private long totalDoctors;
    private long totalSpecialties;

    public DoctorStats(long totalDoctors, long totalSpecialties) {
        this.totalDoctors = totalDoctors;
        this.totalSpecialties = totalSpecialties;
    }

    public long getTotalDoctors() { return totalDoctors; }
    public long getTotalSpecialties() { return totalSpecialties; }
}
