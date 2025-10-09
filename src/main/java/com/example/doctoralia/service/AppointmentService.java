package com.example.doctoralia.service;

import com.example.doctoralia.model.Appointment;
import com.example.doctoralia.model.AppointmentStatus;
import com.example.doctoralia.model.Doctor;
import com.example.doctoralia.model.User;
import com.example.doctoralia.repository.AppointmentRepository;
import com.example.doctoralia.repository.DoctorRepository;
import com.example.doctoralia.repository.UserRepository;
import com.example.doctoralia.dto.CreateAppointmentWithPatientInfoRequest;
import com.example.doctoralia.model.PatientBookingInfo;
import com.example.doctoralia.repository.PatientBookingInfoRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Autowired
    private PatientBookingInfoRepository patientBookingInfoRepository;

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

    /**
     * Confirm an appointment (Doctor only)
     */
    public Appointment confirmAppointment(Long appointmentId, Long doctorId) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Appointment not found");
        }

        Appointment appointment = appointmentOpt.get();

        // Verify that this appointment belongs to the doctor
        if (!appointment.getDoctor().getId().equals(doctorId)) {
            throw new IllegalArgumentException("You can only confirm your own appointments");
        }

        // Check if appointment is in PENDING status
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new IllegalArgumentException("Only pending appointments can be confirmed");
        }

        appointment.setStatus(AppointmentStatus.CONFIRMED);
        Appointment updated = appointmentRepository.save(appointment);

        logger.info("Appointment {} confirmed by doctor {}", appointmentId, doctorId);
        return updated;
    }

    /**
     * Create appointment with complete patient information
     */
    public Map<String, Object> createAppointmentWithPatientInfo(
            CreateAppointmentWithPatientInfoRequest request, Long patientId) {

        // First create the appointment using existing method
        Appointment appointment = createAppointment(
            request.getDoctorId(),
            patientId,
            request.getAppointmentDateTime(),
            request.getDurationMinutes(),
            request.getNotes()
        );

        // Generate queue number if not provided
        String queueNumber = request.getQueueNumber();
        if (queueNumber == null || queueNumber.trim().isEmpty()) {
            queueNumber = generateQueueNumber();
        }

        // Create and save patient booking info
        PatientBookingInfo patientBookingInfo = new PatientBookingInfo(
            appointment,
            request.getPatientPrefix(),
            request.getPatientFirstName(),
            request.getPatientLastName(),
            request.getPatientGender(),
            request.getPatientDateOfBirth(),
            request.getPatientNationality(),
            request.getPatientCitizenId(),
            request.getPatientPhone(),
            request.getPatientEmail(),
            request.getSymptoms(),
            request.getBookingType(),
            queueNumber
        );

        PatientBookingInfo savedPatientInfo = patientBookingInfoRepository.save(patientBookingInfo);

        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Appointment created successfully!");

        Map<String, Object> appointmentData = convertToAppointmentResponse(appointment);
        response.put("appointment", appointmentData);

        Map<String, Object> patientInfo = new HashMap<>();
        patientInfo.put("id", savedPatientInfo.getId());
        patientInfo.put("queueNumber", savedPatientInfo.getQueueNumber());
        patientInfo.put("patientFullName", savedPatientInfo.getPatientFullName());
        patientInfo.put("bookingType", savedPatientInfo.getBookingType());
        patientInfo.put("symptoms", savedPatientInfo.getSymptoms());

        response.put("patientInfo", patientInfo);

        logger.info("Appointment created with patient info: appointmentId={}, patientInfoId={}, queueNumber={}",
                   appointment.getId(), savedPatientInfo.getId(), queueNumber);

        return response;
    }

    /**
     * Generate unique queue number
     */
    private String generateQueueNumber() {
        // Get the latest queue number from database
        String latestQueue = patientBookingInfoRepository.findLatestQueueNumber().orElse("000");

        try {
            int nextNumber = Integer.parseInt(latestQueue) + 1;
            return String.format("%03d", nextNumber); // Format as 3-digit string (001, 002, etc.)
        } catch (NumberFormatException e) {
            logger.warn("Invalid queue number format: {}, starting from 001", latestQueue);
            return "001";
        }
    }

    /**
     * Get patient booking info for an appointment
     */
    public Optional<PatientBookingInfo> getPatientBookingInfo(Long appointmentId) {
        return patientBookingInfoRepository.findByAppointmentId(appointmentId);
    }

    /**
     * Get appointments by doctor and specific date
     */
    public List<Appointment> getAppointmentsByDoctorAndDate(Long doctorId, String dateString) {
        try {
            // Parse date string (YYYY-MM-DD)
            LocalDate date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);

            // Get start and end of day
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);

            // Query appointments for this doctor on this date
            List<Appointment> appointments = appointmentRepository.findByDoctorIdOrderByAppointmentDatetimeAsc(doctorId);

            // Filter by date range
            return appointments.stream()
                    .filter(apt -> {
                        LocalDateTime aptTime = apt.getAppointmentDatetime();
                        return !aptTime.isBefore(startOfDay) && !aptTime.isAfter(endOfDay);
                    })
                    // Only include non-cancelled appointments
                    .filter(apt -> apt.getStatus() != AppointmentStatus.CANCELLED)
                    .toList();

        } catch (Exception e) {
            logger.error("Error getting appointments by doctor and date: ", e);
            throw new IllegalArgumentException("Invalid date format. Please use YYYY-MM-DD");
        }
    }

    /**
     * Convert appointment to response format (reusing existing method logic)
     */
    private Map<String, Object> convertToAppointmentResponse(Appointment appointment) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", appointment.getId());

        // Doctor info
        Map<String, Object> doctor = new HashMap<>();
        doctor.put("id", appointment.getDoctor().getId());
        doctor.put("doctorName", appointment.getDoctor().getDoctorName());
        Map<String, Object> specialty = new HashMap<>();
        specialty.put("id", appointment.getDoctor().getSpecialty().getId());
        specialty.put("name", appointment.getDoctor().getSpecialty().getName());
        doctor.put("specialty", specialty);
        response.put("doctor", doctor);

        // Patient info
        Map<String, Object> patient = new HashMap<>();
        patient.put("id", appointment.getPatient().getId());
        patient.put("email", appointment.getPatient().getEmail());
        patient.put("firstName", appointment.getPatient().getFirstName());
        patient.put("lastName", appointment.getPatient().getLastName());
        response.put("patient", patient);

        response.put("appointmentDatetime", appointment.getAppointmentDatetime());
        response.put("durationMinutes", appointment.getDurationMinutes());
        response.put("status", appointment.getStatus());
        response.put("notes", appointment.getNotes());
        response.put("doctorNotes", appointment.getDoctorNotes());
        response.put("createdAt", appointment.getCreatedAt());
        response.put("updatedAt", appointment.getUpdatedAt());

        return response;
    }
}