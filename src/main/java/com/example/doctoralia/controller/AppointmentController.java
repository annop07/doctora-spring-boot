package com.example.doctoralia.controller;

import com.example.doctoralia.config.JwtUtils;
import com.example.doctoralia.dto.CreateAppointmentRequest;
import com.example.doctoralia.dto.CreateAppointmentWithPatientInfoRequest;
import com.example.doctoralia.dto.MessageResponse;
import com.example.doctoralia.model.Appointment;
import com.example.doctoralia.model.Doctor;
import com.example.doctoralia.service.AppointmentService;
import com.example.doctoralia.service.DoctorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AppointmentController {
    private static final Logger logger = LoggerFactory.getLogger(AppointmentController.class);

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private DoctorService doctorService;

    /**
     * Create a new appointment (Patient only)
     */
    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request,
            HttpServletRequest httpRequest) {
        try {
            String jwt = parseJwt(httpRequest);
            if (jwt == null || !jwtUtils.validateJwtToken(jwt)) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Invalid token"));
            }

            Long patientId = jwtUtils.getUserIdFromJwtToken(jwt);
            logger.info("Creating appointment for patient: {} with doctor: {}", patientId, request.getDoctorId());

            Appointment appointment = appointmentService.createAppointment(
                    request.getDoctorId(),
                    patientId,
                    request.getAppointmentDateTime(),
                    request.getDurationMinutes(),
                    request.getNotes()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Appointment created successfully!");
            response.put("appointment", convertToAppointmentResponse(appointment));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating appointment: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Create appointment with complete patient information (New endpoint)
     */
    @PostMapping("/with-patient-info")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> createAppointmentWithPatientInfo(
            @Valid @RequestBody CreateAppointmentWithPatientInfoRequest request,
            HttpServletRequest httpRequest) {
        try {
            String jwt = parseJwt(httpRequest);
            if (jwt == null || !jwtUtils.validateJwtToken(jwt)) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Invalid token"));
            }

            Long patientId = jwtUtils.getUserIdFromJwtToken(jwt);
            logger.info("Creating appointment with patient info for patient: {} with doctor: {}",
                       patientId, request.getDoctorId());

            // Call new service method that handles patient info
            Map<String, Object> result = appointmentService.createAppointmentWithPatientInfo(
                    request, patientId);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error creating appointment with patient info: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Get all appointments for current patient
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> getMyAppointments(HttpServletRequest request) {
        try {
            String jwt = parseJwt(request);
            if (jwt == null || !jwtUtils.validateJwtToken(jwt)) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Invalid token"));
            }

            Long patientId = jwtUtils.getUserIdFromJwtToken(jwt);
            List<Appointment> appointments = appointmentService.getAppointmentsByPatient(patientId);

            Map<String, Object> response = new HashMap<>();
            response.put("appointments", appointments.stream()
                    .map(this::convertToAppointmentResponse)
                    .toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting appointments: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Get all appointments for current doctor
     */
    @GetMapping("/doctor/my")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> getMyDoctorAppointments(HttpServletRequest request) {
        try {
            String jwt = parseJwt(request);
            if (jwt == null || !jwtUtils.validateJwtToken(jwt)) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Invalid token"));
            }

            Long doctorUserId = jwtUtils.getUserIdFromJwtToken(jwt);

            // Find doctor by user ID first
            Optional<Doctor> doctorOpt = doctorService.findByUserId(doctorUserId);
            if (doctorOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Doctor profile not found"));
            }

            Long doctorId = doctorOpt.get().getId();
            List<Appointment> appointments = appointmentService.getAppointmentsByDoctor(doctorId);

            Map<String, Object> response = new HashMap<>();
            response.put("appointments", appointments.stream()
                    .map(this::convertToAppointmentResponse)
                    .toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting doctor appointments: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Confirm an appointment (Doctor only)
     */
    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> confirmAppointment(
            @PathVariable Long id,
            HttpServletRequest request) {
        logger.info("🔵 [confirmAppointment] Starting - Appointment ID: {}", id);

        try {
            String jwt = parseJwt(request);
            logger.info("🔵 [confirmAppointment] JWT parsed: {}", jwt != null ? "Present" : "NULL");

            if (jwt == null || !jwtUtils.validateJwtToken(jwt)) {
                logger.error("❌ [confirmAppointment] Invalid or missing JWT token");
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Invalid token"));
            }

            Long doctorUserId = jwtUtils.getUserIdFromJwtToken(jwt);
            String role = jwtUtils.getRoleFromJwtToken(jwt);
            logger.info("🔵 [confirmAppointment] Doctor User ID: {}, Role: {}", doctorUserId, role);

            // Find doctor by user ID
            Optional<Doctor> doctorOpt = doctorService.findByUserId(doctorUserId);
            logger.info("🔵 [confirmAppointment] Doctor found: {}", doctorOpt.isPresent());

            if (doctorOpt.isEmpty()) {
                logger.error("❌ [confirmAppointment] Doctor profile not found for user ID: {}", doctorUserId);
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Doctor profile not found"));
            }

            Long doctorId = doctorOpt.get().getId();
            logger.info("🔵 [confirmAppointment] Doctor ID: {}", doctorId);

            Appointment appointment = appointmentService.confirmAppointment(id, doctorId);
            logger.info("✅ [confirmAppointment] Appointment confirmed successfully");

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Appointment confirmed successfully!");
            response.put("appointment", convertToAppointmentResponse(appointment));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ [confirmAppointment] Error: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Get patient booking info for an appointment (Doctor only)
     */
    @GetMapping("/{id}/patient-info")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> getPatientBookingInfo(
            @PathVariable Long id,
            HttpServletRequest request) {
        logger.info("🔵 [getPatientBookingInfo] Getting patient info for appointment ID: {}", id);

        try {
            String jwt = parseJwt(request);
            if (jwt == null || !jwtUtils.validateJwtToken(jwt)) {
                logger.error("❌ [getPatientBookingInfo] Invalid or missing JWT token");
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Invalid token"));
            }

            Long doctorUserId = jwtUtils.getUserIdFromJwtToken(jwt);

            // Find doctor by user ID
            Optional<Doctor> doctorOpt = doctorService.findByUserId(doctorUserId);
            if (doctorOpt.isEmpty()) {
                logger.error("❌ [getPatientBookingInfo] Doctor profile not found for user ID: {}", doctorUserId);
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Doctor profile not found"));
            }

            // Get the appointment to verify it belongs to this doctor
            Appointment appointment = appointmentService.getAppointmentsByDoctor(doctorOpt.get().getId())
                    .stream()
                    .filter(apt -> apt.getId().equals(id))
                    .findFirst()
                    .orElse(null);

            if (appointment == null) {
                logger.error("❌ [getPatientBookingInfo] Appointment not found or doesn't belong to this doctor");
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Appointment not found or unauthorized"));
            }

            // Get patient booking info
            Optional<com.example.doctoralia.model.PatientBookingInfo> patientInfoOpt =
                    appointmentService.getPatientBookingInfo(id);

            if (patientInfoOpt.isEmpty()) {
                logger.warn("⚠️ [getPatientBookingInfo] No patient booking info found for appointment: {}", id);
                return ResponseEntity.ok(new MessageResponse("No patient booking information available"));
            }

            com.example.doctoralia.model.PatientBookingInfo patientInfo = patientInfoOpt.get();

            Map<String, Object> response = new HashMap<>();
            response.put("id", patientInfo.getId());
            response.put("queueNumber", patientInfo.getQueueNumber());
            response.put("patientPrefix", patientInfo.getPatientPrefix());
            response.put("patientFirstName", patientInfo.getPatientFirstName());
            response.put("patientLastName", patientInfo.getPatientLastName());
            response.put("patientFullName", patientInfo.getPatientFullName());
            response.put("patientGender", patientInfo.getPatientGender());
            response.put("patientDateOfBirth", patientInfo.getPatientDateOfBirth());
            response.put("patientNationality", patientInfo.getPatientNationality());
            response.put("patientCitizenId", patientInfo.getPatientCitizenId());
            response.put("patientPhone", patientInfo.getPatientPhone());
            response.put("patientEmail", patientInfo.getPatientEmail());
            response.put("symptoms", patientInfo.getSymptoms());
            response.put("bookingType", patientInfo.getBookingType());
            response.put("createdAt", patientInfo.getCreatedAt());

            logger.info("✅ [getPatientBookingInfo] Patient info retrieved successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ [getPatientBookingInfo] Error: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Cancel an appointment
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('PATIENT') or hasRole('DOCTOR')")
    public ResponseEntity<?> cancelAppointment(
            @PathVariable Long id,
            HttpServletRequest request) {
        try {
            String jwt = parseJwt(request);
            if (jwt == null || !jwtUtils.validateJwtToken(jwt)) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Invalid token"));
            }

            Long userId = jwtUtils.getUserIdFromJwtToken(jwt);
            Appointment appointment = appointmentService.cancelAppointment(id, userId);

            return ResponseEntity.ok(new MessageResponse("Appointment cancelled successfully!"));
        } catch (Exception e) {
            logger.error("Error cancelling appointment: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Get booked time slots for a specific doctor on a specific date (Public API)
     * Returns time slots with their booking status
     */
    @GetMapping("/doctor/{doctorId}/booked-slots")
    public ResponseEntity<?> getBookedTimeSlots(
            @PathVariable Long doctorId,
            @RequestParam String date) { // Format: YYYY-MM-DD
        try {
            logger.info("Getting booked slots for doctor: {} on date: {}", doctorId, date);

            List<Appointment> appointments = appointmentService.getAppointmentsByDoctorAndDate(doctorId, date);

            // Group appointments by time slot and status
            List<Map<String, Object>> bookedSlots = appointments.stream()
                    .map(apt -> {
                        Map<String, Object> slot = new HashMap<>();
                        slot.put("appointmentId", apt.getId());
                        slot.put("startTime", apt.getAppointmentDatetime());
                        slot.put("durationMinutes", apt.getDurationMinutes());
                        slot.put("status", apt.getStatus()); // PENDING, CONFIRMED, CANCELLED, COMPLETED
                        return slot;
                    })
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("doctorId", doctorId);
            response.put("date", date);
            response.put("bookedSlots", bookedSlots);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting booked slots: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }

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