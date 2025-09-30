package com.example.doctoralia.repository;

import com.example.doctoralia.model.Availability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

    // Find all availabilities for a doctor
    List<Availability> findByDoctorIdAndIsActiveTrue(Long doctorId);

    // Find availabilities for a specific day of week
    @Query("SELECT a FROM Availability a WHERE a.doctor.id = :doctorId " +
            "AND a.dayOfWeek = :dayOfWeek AND a.isActive = true " +
            "ORDER BY a.startTime")
    List<Availability> findByDoctorAndDayOfWeek(
            @Param("doctorId") Long doctorId,
            @Param("dayOfWeek") Integer dayOfWeek
    );

    // Find availabilities for a doctor ordered by day and time
    @Query("SELECT a FROM Availability a WHERE a.doctor.id = :doctorId " +
            "AND a.isActive = true " +
            "ORDER BY a.dayOfWeek, a.startTime")
    List<Availability> findByDoctorIdOrderByDayAndTime(@Param("doctorId") Long doctorId);
}