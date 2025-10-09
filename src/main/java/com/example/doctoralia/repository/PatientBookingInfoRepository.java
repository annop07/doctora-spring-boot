package com.example.doctoralia.repository;

import com.example.doctoralia.model.PatientBookingInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientBookingInfoRepository extends JpaRepository<PatientBookingInfo, Long> {

    // Find by appointment ID
    Optional<PatientBookingInfo> findByAppointmentId(Long appointmentId);

    // Find all booking info for a patient (by email)
    List<PatientBookingInfo> findByPatientEmailOrderByCreatedAtDesc(String email);

    // Find by queue number
    Optional<PatientBookingInfo> findByQueueNumber(String queueNumber);

    // Find by citizen ID (for duplicate check)
    List<PatientBookingInfo> findByPatientCitizenIdOrderByCreatedAtDesc(String citizenId);

    // Custom query to get booking info with appointment details
    @Query("""
        SELECT pbi FROM PatientBookingInfo pbi
        JOIN FETCH pbi.appointment a
        JOIN FETCH a.doctor d
        JOIN FETCH d.specialty s
        WHERE pbi.patientEmail = :email
        ORDER BY pbi.createdAt DESC
        """)
    List<PatientBookingInfo> findBookingHistoryByEmail(@Param("email") String email);

    // Get latest queue number for generating next queue
    @Query("SELECT MAX(pbi.queueNumber) FROM PatientBookingInfo pbi WHERE pbi.queueNumber IS NOT NULL")
    Optional<String> findLatestQueueNumber();

    // Find today's bookings
    @Query("""
        SELECT pbi FROM PatientBookingInfo pbi
        JOIN pbi.appointment a
        WHERE DATE_TRUNC('day', a.appointmentDatetime) = DATE_TRUNC('day', CURRENT_TIMESTAMP)
        ORDER BY a.appointmentDatetime ASC
        """)
    List<PatientBookingInfo> findTodaysBookings();

    // Check if citizen ID has existing booking for same day
    @Query("""
        SELECT COUNT(pbi) FROM PatientBookingInfo pbi
        JOIN pbi.appointment a
        WHERE pbi.patientCitizenId = :citizenId
        AND DATE_TRUNC('day', a.appointmentDatetime) = DATE_TRUNC('day', :appointmentDate)
        """)
    long countByPatientCitizenIdAndAppointmentDate(
        @Param("citizenId") String citizenId,
        @Param("appointmentDate") java.time.LocalDateTime appointmentDate
    );
}