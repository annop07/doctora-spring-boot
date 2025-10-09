package com.example.doctoralia.repository;

import com.example.doctoralia.model.Appointment;
import com.example.doctoralia.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Find appointments by patient
    List<Appointment> findByPatientIdOrderByAppointmentDatetimeDesc(Long patientId);

    // Find appointments by doctor
    List<Appointment> findByDoctorIdOrderByAppointmentDatetimeAsc(Long doctorId);

    // Find appointments by status
    List<Appointment> findByStatus(AppointmentStatus status);

    // Find upcoming appointments for a patient
    @Query("SELECT a FROM Appointment a WHERE a.patient.id = :patientId " +
            "AND a.appointmentDatetime > :now " +
            "AND a.status IN ('PENDING', 'CONFIRMED') " +
            "ORDER BY a.appointmentDatetime ASC")
    List<Appointment> findUpcomingAppointmentsByPatient(
            @Param("patientId") Long patientId,
            @Param("now") LocalDateTime now);

    // Find appointments for a doctor on a specific date
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId " +
            "AND DATE(a.appointmentDatetime) = DATE(:date) " +
            "ORDER BY a.appointmentDatetime ASC")
    List<Appointment> findByDoctorIdAndDate(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDateTime date);

    // Check for conflicting appointments (for preventing double booking)
    // Using native PostgreSQL query for better compatibility
    @Query(value = "SELECT * FROM appointments a WHERE a.doctor_id = :doctorId " +
            "AND a.status IN ('PENDING', 'CONFIRMED') " +
            "AND ((a.appointment_datetime <= :start AND " +
            "      a.appointment_datetime + (a.duration_minutes || ' minutes')::interval > :start) " +
            "OR (a.appointment_datetime < :end AND " +
            "    a.appointment_datetime + (a.duration_minutes || ' minutes')::interval >= :end) " +
            "OR (a.appointment_datetime >= :start AND " +
            "    a.appointment_datetime < :end))",
            nativeQuery = true)
    List<Appointment> findConflictingAppointments(
            @Param("doctorId") Long doctorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Count appointments by status
    long countByStatus(AppointmentStatus status);

    // Count appointments for a doctor
    long countByDoctorId(Long doctorId);
}