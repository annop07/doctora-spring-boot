package com.example.doctoralia.service;

import com.example.doctoralia.model.*;
import com.example.doctoralia.repository.AppointmentRepository;
import com.example.doctoralia.repository.DoctorRepository;
import com.example.doctoralia.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AppointmentService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    /**
     * จองนัดหมาย
     */
    public Appointment bookAppointment(Long patientId, Long doctorId, LocalDateTime appointmentDateTime,
                                     Integer durationMinutes, String notes) {

        // หาคนไข้
        Optional<User> patientOpt = userRepository.findById(patientId);
        if (patientOpt.isEmpty()) {
            throw new IllegalArgumentException("Patient not found");
        }
        User patient = patientOpt.get();

        // หาหมอ
        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
        if (doctorOpt.isEmpty()) {
            throw new IllegalArgumentException("Doctor not found");
        }
        Doctor doctor = doctorOpt.get();

        // ตรวจสอบว่าหมอยังทำงานอยู่
        if (!doctor.getIsActive()) {
            throw new IllegalArgumentException("Doctor is not active");
        }

        // ตรวจสอบเวลาในอดีต
        if (appointmentDateTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot book appointment in the past");
        }

        // ตรวจสอบเวลาซ้อนกัน
        LocalDateTime endTime = appointmentDateTime.plusMinutes(durationMinutes != null ? durationMinutes : 30);
        List<Appointment> overlapping = appointmentRepository.findOverlappingAppointments(
                doctorId, appointmentDateTime, endTime, 0L
        );

        if (!overlapping.isEmpty()) {
            throw new IllegalArgumentException("Time slot is not available");
        }

        // ตรวจสอบว่าคนไข้มีนัดกับหมอคนนี้แล้วหรือไม่
        List<Appointment> existing = appointmentRepository.findExistingFutureAppointments(
                patientId, doctorId, LocalDateTime.now()
        );

        if (!existing.isEmpty()) {
            throw new IllegalArgumentException("You already have a pending/confirmed appointment with this doctor");
        }

        // สร้างนัดหมาย
        Appointment appointment = new Appointment(doctor, patient, appointmentDateTime);
        appointment.setDurationMinutes(durationMinutes != null ? durationMinutes : 30);
        appointment.setNotes(notes);
        appointment.setStatus(AppointmentStatus.PENDING);

        Appointment saved = appointmentRepository.save(appointment);
        logger.info("Appointment booked: Patient {} with Doctor {} at {}",
                   patient.getFullName(), doctor.getDoctorName(), appointmentDateTime);

        return saved;
    }

    /**
     * ดูนัดหมายของคนไข้
     */
    public List<Appointment> getPatientAppointments(Long patientId) {
        Optional<User> patientOpt = userRepository.findById(patientId);
        if (patientOpt.isEmpty()) {
            throw new IllegalArgumentException("Patient not found");
        }

        return appointmentRepository.findByPatientOrderByAppointmentDateTimeDesc(patientOpt.get());
    }

    /**
     * ดูนัดหมายของหมอ
     */
    public List<Appointment> getDoctorAppointments(Long doctorId) {
        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
        if (doctorOpt.isEmpty()) {
            throw new IllegalArgumentException("Doctor not found");
        }

        return appointmentRepository.findByDoctorOrderByAppointmentDateTimeAsc(doctorOpt.get());
    }

    /**
     * ยกเลิกนัดหมาย (คนไข้)
     */
    public Appointment cancelAppointment(Long patientId, Long appointmentId) {
        Optional<User> patientOpt = userRepository.findById(patientId);
        if (patientOpt.isEmpty()) {
            throw new IllegalArgumentException("Patient not found");
        }

        Optional<Appointment> appointmentOpt = appointmentRepository.findByIdAndPatient(appointmentId, patientOpt.get());
        if (appointmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Appointment not found or access denied");
        }

        Appointment appointment = appointmentOpt.get();

        if (!appointment.canBeCancelled()) {
            throw new IllegalArgumentException("Cannot cancel this appointment");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);

        Appointment updated = appointmentRepository.save(appointment);
        logger.info("Appointment cancelled by patient: {}", appointment.getId());

        return updated;
    }

    /**
     * อนุมัติการนัด (หมอ)
     */
    public Appointment confirmAppointment(Long doctorId, Long appointmentId) {
        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
        if (doctorOpt.isEmpty()) {
            throw new IllegalArgumentException("Doctor not found");
        }

        Optional<Appointment> appointmentOpt = appointmentRepository.findByIdAndDoctor(appointmentId, doctorOpt.get());
        if (appointmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Appointment not found or access denied");
        }

        Appointment appointment = appointmentOpt.get();

        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new IllegalArgumentException("Only pending appointments can be confirmed");
        }

        appointment.setStatus(AppointmentStatus.CONFIRMED);

        Appointment updated = appointmentRepository.save(appointment);
        logger.info("Appointment confirmed by doctor: {}", appointment.getId());

        return updated;
    }

    /**
     * ปฏิเสธการนัด (หมอ)
     */
    public Appointment rejectAppointment(Long doctorId, Long appointmentId, String reason) {
        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
        if (doctorOpt.isEmpty()) {
            throw new IllegalArgumentException("Doctor not found");
        }

        Optional<Appointment> appointmentOpt = appointmentRepository.findByIdAndDoctor(appointmentId, doctorOpt.get());
        if (appointmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Appointment not found or access denied");
        }

        Appointment appointment = appointmentOpt.get();

        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new IllegalArgumentException("Only pending appointments can be rejected");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setDoctorNotes(reason);

        Appointment updated = appointmentRepository.save(appointment);
        logger.info("Appointment rejected by doctor: {} - Reason: {}", appointment.getId(), reason);

        return updated;
    }
}