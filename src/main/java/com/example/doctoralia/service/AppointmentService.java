package com.example.doctoralia.service;

import com.example.doctoralia.model.Appointment;
import com.example.doctoralia.model.AppointmentStatus;
import com.example.doctoralia.model.Doctor;
import com.example.doctoralia.model.User;
import com.example.doctoralia.repository.AppointmentRepository;
import com.example.doctoralia.repository.DoctorRepository;
import com.example.doctoralia.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AppointmentService {
    private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private UserRepository userRepository;

    public Appointment createAppointment(Long doctorId, Long patientId,
                                         LocalDateTime appointmentDateTime,
                                         Integer durationMinutes, String notes) {
        // Validate doctor exists and is active
        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
        if (doctorOpt.isEmpty()) {
            throw new IllegalArgumentException("Doctor not found with ID: " + doctorId);
        }

        Doctor doctor = doctorOpt.get();
        if (!doctor.getIsActive()) {
            throw new IllegalArgumentException("Doctor is not active");
        }

        // Validate patient exists
        Optional<User> patientOpt = userRepository.findById(patientId);
        if (patientOpt.isEmpty()) {
            throw new IllegalArgumentException("Patient not found with ID: " + patientId);
        }

        User patient = patientOpt.get();

        // Check if appointment time is in the future
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        if (appointmentDateTime.isBefore(now)) {
            throw new IllegalArgumentException("Appointment time must be in the future");
        }

        // Check for conflicting appointments (same doctor, overlapping time)
        LocalDateTime appointmentEnd = appointmentDateTime.plusMinutes(durationMinutes != null ? durationMinutes : 30);
        List<Appointment> conflicts = appointmentRepository.findConflictingAppointments(
                doctorId, appointmentDateTime, appointmentEnd);

        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("This time slot is not available. Please choose another time.");
        }

        // Create new appointment
        Appointment appointment = new Appointment();
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setAppointmentDatetime(appointmentDateTime);
        appointment.setDurationMinutes(durationMinutes != null ? durationMinutes : 30);
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setNotes(notes);

        Appointment savedAppointment = appointmentRepository.save(appointment);
        logger.info("Appointment created: {} for patient {} with doctor {}",
                savedAppointment.getId(), patientId, doctorId);

        return savedAppointment;
    }

    public List<Appointment> getAppointmentsByPatient(Long patientId) {
        return appointmentRepository.findByPatientIdOrderByAppointmentDatetimeDesc(patientId);
    }

    public List<Appointment> getAppointmentsByDoctor(Long doctorId) {
        return appointmentRepository.findByDoctorIdOrderByAppointmentDatetimeAsc(doctorId);
    }

    public Appointment cancelAppointment(Long appointmentId, Long userId) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Appointment not found");
        }

        Appointment appointment = appointmentOpt.get();

        // Check if user is authorized to cancel
        if (!appointment.getPatient().getId().equals(userId) &&
                !appointment.getDoctor().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Not authorized to cancel this appointment");
        }

        // Can only cancel PENDING or CONFIRMED appointments
        if (appointment.getStatus() != AppointmentStatus.PENDING &&
                appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new IllegalArgumentException("Cannot cancel appointment with status: " + appointment.getStatus());
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment updated = appointmentRepository.save(appointment);

        logger.info("Appointment {} cancelled by user {}", appointmentId, userId);
        return updated;
    }

    public Appointment updateAppointmentStatus(Long appointmentId, AppointmentStatus status) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Appointment not found");
        }

        Appointment appointment = appointmentOpt.get();
        appointment.setStatus(status);

        return appointmentRepository.save(appointment);
    }
}