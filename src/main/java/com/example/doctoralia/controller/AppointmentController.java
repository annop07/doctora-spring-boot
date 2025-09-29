package com.example.doctoralia.controller;

import com.example.doctoralia.config.JwtUtils;
import com.example.doctoralia.dto.CreateAppointmentRequest;
import com.example.doctoralia.dto.MessageResponse;
import com.example.doctoralia.model.Appointment;
import com.example.doctoralia.service.AppointmentService;
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

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AppointmentController {
    private static final Logger logger = LoggerFactory.getLogger(AppointmentController.class);

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private JwtUtils jwtUtils;

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