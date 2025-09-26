package com.example.doctoralia.controller;

import com.example.doctoralia.config.JwtUtils;
import com.example.doctoralia.dto.BookAppointmentRequest;
import com.example.doctoralia.dto.MessageResponse;
import com.example.doctoralia.dto.RejectAppointmentRequest;
import com.example.doctoralia.model.Appointment;
import com.example.doctoralia.model.Doctor;
import com.example.doctoralia.model.UserRole;
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
    private DoctorService doctorService;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * จองนัดหมาย (คนไข้เท่านั้น)
     * POST /api/appointments
     */
    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> bookAppointment(@Valid @RequestBody BookAppointmentRequest request,
                                             HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            String role = getRoleFromToken(httpRequest);

            if (!UserRole.PATIENT.name().equals(role)) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Access denied. Patient role required."));
            }

            Appointment appointment = appointmentService.bookAppointment(
                    userId, // patientId
                    request.getDoctorId(),
                    request.getAppointmentDateTime(),
                    request.getDurationMinutes(),
                    request.getNotes()
            );

            Map<String, Object> response = convertToAppointmentResponse(appointment);
            response.put("message", "Appointment booked successfully! Waiting for doctor approval.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error booking appointment: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * ดูนัดหมายของคนไข้ตัวเอง (คนไข้เท่านั้น)
     * GET /api/appointments/my
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> getMyAppointments(HttpServletRequest request) {
        try {
            Long userId = getUserIdFromToken(request);

            List<Appointment> appointments = appointmentService.getPatientAppointments(userId);

            List<Map<String, Object>> response = appointments.stream()
                    .map(this::convertToAppointmentResponse)
                    .toList();

            return ResponseEntity.ok(Map.of("appointments", response));

        } catch (Exception e) {
            logger.error("Error getting patient appointments: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * ยกเลิกนัดหมาย (คนไข้เท่านั้น)
     * PUT /api/appointments/{id}/cancel
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> cancelAppointment(@PathVariable Long id,
                                               HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);

            Appointment appointment = appointmentService.cancelAppointment(userId, id);

            return ResponseEntity.ok(new MessageResponse("Appointment cancelled successfully!"));

        } catch (Exception e) {
            logger.error("Error cancelling appointment: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * ดูนัดหมายของหมอ (หมอเท่านั้น)
     * GET /api/appointments/my-patients
     */
    @GetMapping("/my-patients")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> getDoctorAppointments(HttpServletRequest request) {
        try {
            Long userId = getUserIdFromToken(request);

            // หา doctor profile
            Optional<Doctor> doctorOpt = doctorService.findByUserId(userId);
            if (doctorOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Doctor profile not found"));
            }

            Doctor doctor = doctorOpt.get();
            List<Appointment> appointments = appointmentService.getDoctorAppointments(doctor.getId());

            List<Map<String, Object>> response = appointments.stream()
                    .map(this::convertToDoctorAppointmentResponse)
                    .toList();

            return ResponseEntity.ok(Map.of("appointments", response));

        } catch (Exception e) {
            logger.error("Error getting doctor appointments: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * อนุมัติการนัด (หมอเท่านั้น)
     * PUT /api/appointments/{id}/confirm
     */
    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> confirmAppointment(@PathVariable Long id,
                                                HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);

            // หา doctor profile
            Optional<Doctor> doctorOpt = doctorService.findByUserId(userId);
            if (doctorOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Doctor profile not found"));
            }

            Doctor doctor = doctorOpt.get();
            Appointment appointment = appointmentService.confirmAppointment(doctor.getId(), id);

            return ResponseEntity.ok(new MessageResponse("Appointment confirmed successfully!"));

        } catch (Exception e) {
            logger.error("Error confirming appointment: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * ปฏิเสธการนัด (หมอเท่านั้น)
     * PUT /api/appointments/{id}/reject
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> rejectAppointment(@PathVariable Long id,
                                               @RequestBody RejectAppointmentRequest request,
                                               HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);

            // หา doctor profile
            Optional<Doctor> doctorOpt = doctorService.findByUserId(userId);
            if (doctorOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Doctor profile not found"));
            }

            Doctor doctor = doctorOpt.get();
            Appointment appointment = appointmentService.rejectAppointment(
                    doctor.getId(),
                    id,
                    request.getReason()
            );

            return ResponseEntity.ok(new MessageResponse("Appointment rejected successfully!"));

        } catch (Exception e) {
            logger.error("Error rejecting appointment: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    // Helper methods
    private Long getUserIdFromToken(HttpServletRequest request) {
        String jwt = parseJwt(request);
        return jwtUtils.getUserIdFromJwtToken(jwt);
    }

    private String getRoleFromToken(HttpServletRequest request) {
        String jwt = parseJwt(request);
        return jwtUtils.getRoleFromJwtToken(jwt);
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
        response.put("appointmentDateTime", appointment.getAppointmentDateTime());
        response.put("durationMinutes", appointment.getDurationMinutes());
        response.put("status", appointment.getStatus());
        response.put("notes", appointment.getNotes());
        response.put("createdAt", appointment.getCreatedAt());

        // Doctor info
        Map<String, Object> doctor = new HashMap<>();
        doctor.put("id", appointment.getDoctor().getId());
        doctor.put("name", appointment.getDoctorName());
        doctor.put("specialty", appointment.getDoctor().getSpecialtyName());
        doctor.put("roomNumber", appointment.getDoctor().getRoomNumber());
        doctor.put("consultationFee", appointment.getDoctor().getConsultationFee());
        response.put("doctor", doctor);

        return response;
    }

    private Map<String, Object> convertToDoctorAppointmentResponse(Appointment appointment) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", appointment.getId());
        response.put("appointmentDateTime", appointment.getAppointmentDateTime());
        response.put("durationMinutes", appointment.getDurationMinutes());
        response.put("status", appointment.getStatus());
        response.put("notes", appointment.getNotes());
        response.put("doctorNotes", appointment.getDoctorNotes());
        response.put("createdAt", appointment.getCreatedAt());

        // Patient info
        Map<String, Object> patient = new HashMap<>();
        patient.put("id", appointment.getPatient().getId());
        patient.put("name", appointment.getPatientName());
        patient.put("email", appointment.getPatient().getEmail());
        patient.put("phone", appointment.getPatient().getPhone());
        response.put("patient", patient);

        return response;
    }


}
