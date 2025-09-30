package com.example.doctoralia.controller;

import com.example.doctoralia.config.JwtUtils;
import com.example.doctoralia.dto.AddAvailabilityRequest;
import com.example.doctoralia.dto.MessageResponse;
import com.example.doctoralia.model.Appointment;
import com.example.doctoralia.model.Availability;
import com.example.doctoralia.model.Doctor;
import com.example.doctoralia.model.UserRole;
import com.example.doctoralia.repository.AppointmentRepository;
import com.example.doctoralia.service.AvailabilityService;
import com.example.doctoralia.service.DoctorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/availability")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AvailabilityController {
    private static final Logger logger = LoggerFactory.getLogger(AvailabilityController.class);

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private AvailabilityService availabilityService;




    //เพิ่มตารางเวลาของหมอ (หมอเท่านั้น)

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> addAvailability(@Valid @RequestBody AddAvailabilityRequest request,
                                             HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            String role = getRoleFromToken(httpRequest);

            if (!UserRole.DOCTOR.name().equals(role)) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Access denied. Doctor role required."));
            }

            //หา doctor profile
            Optional<Doctor> doctorOpt = doctorService.findByUserId(userId);
            if (doctorOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Doctor profile not found."));
            }

            Doctor doctor = doctorOpt.get();

            Availability availability = availabilityService.addAvailability(
                    doctor.getId(),
                    request.getDayOfWeek(),
                    request.getStartTime(),
                    request.getEndTime()
            );

            Map<String, Object> response = convertToAvailabilityResponse(availability);
            response.put("message", "Availability added successfully!");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error adding availability!", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }

    }


    /**
     * ดูตารางเวลาของหมอตัวเอง (หมอเท่านั้น)
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> getMyAvailability(HttpServletRequest request) {
        try{
            Long userId = getUserIdFromToken(request);

            //หา doctor profile
            Optional<Doctor> doctorOpt = doctorService.findByUserId(userId);
            if (doctorOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Doctor profile not found."));
            }

            Doctor doctor = doctorOpt.get();
            List<Availability> availabilities = availabilityService.getDoctorAvailabilities(doctor.getId());

            List<Map<String,Object>> response = availabilities.stream()
                    .map(this::convertToAvailabilityResponse)
                    .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e){
            logger.error("Error getting availability!", e);
            return  ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    //ตารางเวลาของหมอคนใดคนหนึ่ง
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<?> getDoctorAvailabilities(@PathVariable Long doctorId,
                                                     @RequestParam(required = false) Integer dayOfWeek) {

        try{
            List<Availability> availabilities;

            if (dayOfWeek != null) {
                availabilities = availabilityService.getDoctorAvailabilitiesByDay(doctorId, dayOfWeek);
            } else {
                availabilities = availabilityService.getDoctorAvailabilities(doctorId);
            }

            List<Map<String,Object>> response = availabilities.stream()
                    .map(this::convertToPublicAvailabilityResponse)
                    .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting availability!", e);
            return  ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    //แก้ไขตารางเวลาหมอ (หมอเท่านั้น)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> updateAvailability (@PathVariable Long id,
                                                 @Valid @RequestBody AddAvailabilityRequest request,
                                                 HttpServletRequest httpRequest) {
        try{
            Long userId = getUserIdFromToken(httpRequest);

            Optional<Doctor> doctorOpt = doctorService.findByUserId(userId);
            if (doctorOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Doctor profile not found."));
            }

            Doctor doctor = doctorOpt.get();

            Availability availability = availabilityService.updateAvailability(
                    doctor.getId(),
                    id,
                    request.getDayOfWeek(),
                    request.getStartTime(),
                    request.getEndTime()
            );

            return ResponseEntity.ok(new MessageResponse("Availability updated successfully!"));
        } catch (Exception e) {
            logger.error("Error updating availability!", e);
            return  ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    //ลบตารางเวลา
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> deleteAvailability (@PathVariable Long id,
                                                 HttpServletRequest httpRequest) {
        try{
            Long userId = getUserIdFromToken(httpRequest);

            Optional<Doctor> doctorOpt = doctorService.findByUserId(userId);
            if (doctorOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Doctor profile not found."));
            }

            Doctor doctor = doctorOpt.get();

            availabilityService.deleteAvailability(doctor.getId(), id);

            return ResponseEntity.ok(new MessageResponse("Availability deleted successfully!"));
        } catch (Exception e) {
            logger.error("Error deleting availability!", e);
            return  ResponseEntity.badRequest()
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

    private Map<String, Object> convertToAvailabilityResponse(Availability availability) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", availability.getId());
        response.put("dayOfWeek", availability.getDayOfWeek());
        response.put("dayName", availability.getDayName());
        response.put("startTime", availability.getStartTime());
        response.put("endTime", availability.getEndTime());
        response.put("timeRange", availability.getTimeRange());
        response.put("isActive", availability.getIsActive());
        response.put("createdAt", availability.getCreatedAt());
        return response;
    }

    private Map<String, Object> convertToPublicAvailabilityResponse(Availability availability) {
        Map<String, Object> response = new HashMap<>();
        response.put("dayOfWeek", availability.getDayOfWeek());
        response.put("dayName", availability.getDayName());
        response.put("startTime", availability.getStartTime());
        response.put("endTime", availability.getEndTime());
        response.put("timeRange", availability.getTimeRange());
        return response;
    }
}
