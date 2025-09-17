package com.example.doctoralia.repository;

import com.example.doctoralia.model.Appointment;
import com.example.doctoralia.model.AppointmentStatus;
import com.example.doctoralia.model.Doctor;
import com.example.doctoralia.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment,Long> {

    /**
     * หานัดหมายของคนไข้
     */
    List<Appointment> findByPatientOrderByAppointmentDateTimeDesc(User patient);

    /**
     * หานัดหมายของคนไข้ตาม status
     */
    List<Appointment> findByPatientAndStatusOrderByAppointmentDateTimeDesc(User patient, AppointmentStatus status);

    /**
     * หานัดหมายของคนไข้ (pagination)
     */
    Page<Appointment> findByPatientOrderByAppointmentDateTimeDesc(User patient, Pageable pageable);

    /**
     * หานัดหมายของหมอ
     */
    List<Appointment> findByDoctorOrderByAppointmentDateTimeAsc(Doctor doctor);

    /**
     * หานัดหมายของหมอตาม status
     */
    List<Appointment> findByDoctorAndStatusOrderByAppointmentDateTimeAsc(Doctor doctor, AppointmentStatus status);

    /**
     * หานัดหมายของหมอ (pagination)
     */
    Page<Appointment> findByDoctorOrderByAppointmentDateTimeAsc(Doctor doctor, Pageable pageable);

    /**
     * หานัดหมายของหมอในช่วงเวลา
     */
    List<Appointment> findByDoctorAndAppointmentDateTimeBetween(Doctor doctor, LocalDateTime start, LocalDateTime end);

    /**
     * ตรวจสอบเวลาซ้อนกันของหมอ
     */
    @Query(value = "SELECT * FROM appointments a WHERE " +
            "a.doctor_id = :doctorId AND " +
            "a.status IN ('PENDING', 'CONFIRMED') AND " +
            "a.id <> :excludeId AND " +
            "((a.appointment_datetime <= :startTime AND " +
            "  a.appointment_datetime + INTERVAL '1 minute' * a.duration_minutes > :startTime) OR " +
            "(a.appointment_datetime < :endTime AND " +
            "  a.appointment_datetime + INTERVAL '1 minute' * a.duration_minutes >= :endTime) OR " +
            "(a.appointment_datetime >= :startTime AND " +
            "  a.appointment_datetime + INTERVAL '1 minute' * a.duration_minutes <= :endTime))", nativeQuery = true)
    List<Appointment> findOverlappingAppointments(@Param("doctorId") Long doctorId,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime,
                                                  @Param("excludeId") Long excludeId);

    /**
     * หานัดหมายที่จะมาถึงเร็วๆ นี้ (สำหรับ notification)
     */
    @Query("SELECT a FROM Appointment a WHERE " +
            "a.status = 'CONFIRMED' AND " +
            "a.appointmentDateTime BETWEEN :now AND :futureTime " +
            "ORDER BY a.appointmentDateTime ASC")
    List<Appointment> findUpcomingAppointments(@Param("now") LocalDateTime now,
                                               @Param("futureTime") LocalDateTime futureTime);

    /**
     * หานัดหมายตาม ID และ patient (สำหรับ security)
     */
    Optional<Appointment> findByIdAndPatient(Long id, User patient);

    /**
     * หานัดหมายตาม ID และ doctor (สำหรับ security)
     */
    Optional<Appointment> findByIdAndDoctor(Long id, Doctor doctor);

    /**
     * นับจำนวนนัดหมายของหมอตาม status
     */
    long countByDoctorAndStatus(Doctor doctor, AppointmentStatus status);

    /**
     * นับจำนวนนัดหมายของคนไข้ตาม status
     */
    long countByPatientAndStatus(User patient, AppointmentStatus status);

    /**
     * หานัดหมายของหมอในวันที่กำหนด
     */
    @Query(value = "SELECT * FROM appointments a WHERE " +
            "a.doctor_id = :doctorId AND " +
            "DATE(a.appointment_datetime) = DATE(:date) " +
            "ORDER BY a.appointment_datetime ASC", nativeQuery = true)
    List<Appointment> findByDoctorAndDate(@Param("doctorId") Long doctorId,
                                          @Param("date") LocalDateTime date);

    /**
     * หานัดหมายที่คนไข้มีกับหมอคนนี้แล้ว (เพื่อป้องกันจองซ้ำ)
     */
    @Query("SELECT a FROM Appointment a WHERE " +
            "a.patient.id = :patientId AND " +
            "a.doctor.id = :doctorId AND " +
            "a.status IN ('PENDING', 'CONFIRMED') AND " +
            "a.appointmentDateTime > :now")
    List<Appointment> findExistingFutureAppointments(@Param("patientId") Long patientId,
                                                     @Param("doctorId") Long doctorId,
                                                     @Param("now") LocalDateTime now);

}
