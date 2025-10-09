package com.example.doctoralia.controller;

import com.example.doctoralia.config.JwtUtils;
import com.example.doctoralia.dto.DoctorStats;
import com.example.doctoralia.model.Appointment;
import com.example.doctoralia.model.AppointmentStatus;
import com.example.doctoralia.model.Doctor;
import com.example.doctoralia.model.Specialty;
import com.example.doctoralia.repository.DoctorRepository;
import com.example.doctoralia.repository.SpecialtyRepository;
import com.example.doctoralia.service.AppointmentService;
import com.example.doctoralia.service.AvailabilityService;
import com.example.doctoralia.service.DoctorService;
import com.example.doctoralia.service.SpecialtyService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.Random;

@RestController
@RequestMapping("/api/doctors")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DoctorController {

    private static Logger logger = LoggerFactory.getLogger(DoctorController.class);

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private SpecialtyService specialtyService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AvailabilityService availabilityService;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * Get all active doctors (for general listing)
     */
    @GetMapping("/active")
    public ResponseEntity<?> getAllActiveDoctors() {
        try {
            List<Doctor> doctors = doctorService.findByIsActiveTrue();

            List<Map<String, Object>> doctorList = doctors.stream()
                .map(this::convertToSimpleDoctorResponse)
                .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("doctors", doctorList);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching active doctors: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch doctors"));
        }
    }

    /**
     * Get doctors by specialty name (for specialty-based selection)
     */
    @GetMapping("/by-specialty")
    public ResponseEntity<?> getDoctorsBySpecialty(@RequestParam String specialty) {
        try {
            List<Doctor> doctors = doctorService.findBySpecialtyName(specialty);

            List<Map<String, Object>> doctorList = doctors.stream()
                .map(this::convertToSimpleDoctorResponse)
                .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("doctors", doctorList);
            response.put("specialty", specialty);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching doctors by specialty: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch doctors for specialty: " + specialty));
        }
    }


    /**
     * Improved search endpoint with better error handling
     */
    @GetMapping
    public ResponseEntity<?> searchDoctors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long specialty,
            @RequestParam(required = false) BigDecimal minFee,
            @RequestParam(required = false) BigDecimal maxFee,
            @RequestParam(required = false, defaultValue = "false") boolean includeInactive) {

        try {
            logger.info("Searching doctors with params: page={}, size={}, name={}, specialty={}, minFee={}, maxFee={}",
                    page, size, name, specialty, minFee, maxFee);

            Page<Doctor> doctors;

            // If we have search/filter parameters, use advanced search
            if (name != null || specialty != null || minFee != null || maxFee != null) {
                try {
                    if (includeInactive) {
                        doctors = doctorService.searchDoctorsIncludingInactive(name, specialty, minFee, maxFee, page, size);
                    } else {
                        doctors = doctorService.searchDoctors(name, specialty, minFee, maxFee, page, size);
                    }
                } catch (Exception e) {
                    logger.warn("Advanced search failed, falling back to simpler methods: {}", e.getMessage());

                    // Fallback: Try individual filters
                    if (specialty != null && name == null) {
                        // Specialty only
                        doctors = doctorService.findBySpecialty(specialty, page, size);
                    } else if (name != null && specialty == null) {
                        // Name only - convert List to Page
                        List<Doctor> doctorList = doctorService.findByName(name);
                        int start = page * size;
                        int end = Math.min(start + size, doctorList.size());
                        List<Doctor> pageContent = doctorList.subList(start, end);
                        doctors = new PageImpl<>(pageContent, PageRequest.of(page, size), doctorList.size());
                    } else {
                        // Get all doctors
                        if (includeInactive) {
                            doctors = doctorService.getAllDoctorsIncludingInactive(page, size, sort);
                        } else {
                            doctors = doctorService.getAllDoctors(page, size, sort);
                        }
                    }
                }
            } else {
                // No filters, get all doctors
                if (includeInactive) {
                    doctors = doctorService.getAllDoctorsIncludingInactive(page, size, sort);
                } else {
                    doctors = doctorService.getAllDoctors(page, size, sort);
                }
            }

            // Convert to response format
            Map<String, Object> response = new HashMap<>();
            response.put("doctors", doctors.getContent().stream().map(this::convertToDoctorResponse).toList());
            response.put("currentPage", doctors.getNumber());
            response.put("totalItems", doctors.getTotalElements());
            response.put("totalPages", doctors.getTotalPages());
            response.put("hasNext", doctors.hasNext());
            response.put("hasPrevious", doctors.hasPrevious());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error searching doctors: ", e);

            // Return structured error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to search doctors");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("doctors", new ArrayList<>());
            errorResponse.put("currentPage", page);
            errorResponse.put("totalItems", 0);
            errorResponse.put("totalPages", 0);
            errorResponse.put("hasNext", false);
            errorResponse.put("hasPrevious", false);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    //ดูโปรไฟล์หมอ (Public api) ใครก็ดูได้
    @GetMapping("/{id}")
    public ResponseEntity<?> getDoctorById(@PathVariable Long id) {
        try {
            Optional<Doctor> doctorOpt = doctorService.findById(id);

            if (doctorOpt.isPresent()) {
                Doctor doctor = doctorOpt.get();

                // For public access, only show active doctors
                if (!doctor.getIsActive()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Doctor is not available"));
                }

                return ResponseEntity.ok(convertToDoctorDetailResponse(doctor));

            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Error getting doctor by ID: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Error getting doctor: " + e.getMessage()));
        }
    }

    //ดึงหมอตาม specialty (Public API) - เฉพาะ active doctors
    @GetMapping("/specialty/{specialtyId}")
    public ResponseEntity<?> getDoctorsBySpecialty(
            @PathVariable Long specialtyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            // ตรวจสอบ specialty มีอยู่
            Optional<Specialty> specialtyOpt = specialtyService.findById(specialtyId);
            if (specialtyOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Specialty not found"));
            }

            // Only show active doctors for public API
            Page<Doctor> doctors = doctorService.findBySpecialty(specialtyId, page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("specialty", convertToSpecialtyResponse(specialtyOpt.get()));
            response.put("doctors", doctors.getContent().stream().map(this::convertToDoctorResponse).toList());
            response.put("currentPage", doctors.getNumber());
            response.put("totalItems", doctors.getTotalElements());
            response.put("totalPages", doctors.getTotalPages());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting doctors by specialty: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Error getting doctors: " + e.getMessage()));
        }
    }

    //ค้นหาตามชื่อ (public API) - เฉพาะ active doctors
    @GetMapping("/search")
    public ResponseEntity<?> searchDoctorsByName(@RequestParam String name) {
        try {
            // Only show active doctors for public search
            List<Doctor> doctors = doctorService.findByName(name);

            List<Map<String, Object>> response = doctors.stream()
                    .map(this::convertToDoctorResponse)
                    .toList();

            return ResponseEntity.ok(Map.of("doctors", response));

        } catch (Exception e) {
            logger.error("Error searching doctors by name: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Error searching doctors: " + e.getMessage()));
        }
    }

    //ดึงสถิติหมอ (Public API)
    @GetMapping("/stats")
    public ResponseEntity<?> getDoctorStats() {
        try {
            DoctorStats stats = doctorService.getDoctorStats();
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("Error getting doctor stats: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Error getting stats: " + e.getMessage()));
        }
    }

    /**
     * Get current doctor's profile (Doctor only)
     */
    @GetMapping("/profile/my")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> getMyProfile(HttpServletRequest request) {
        try {
            String jwt = parseJwt(request);
            if (jwt == null || !jwtUtils.validateJwtToken(jwt)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid token"));
            }

            Long doctorUserId = jwtUtils.getUserIdFromJwtToken(jwt);
            logger.info("Getting profile for doctor user ID: {}", doctorUserId);

            Optional<Doctor> doctorOpt = doctorService.findByUserId(doctorUserId);
            if (doctorOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Doctor profile not found"));
            }

            Doctor doctor = doctorOpt.get();
            return ResponseEntity.ok(convertToDoctorDetailResponse(doctor));

        } catch (Exception e) {
            logger.error("Error getting doctor profile: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Error: " + e.getMessage()));
        }
    }

    /**
     * Update current doctor's profile (Doctor only)
     */
    @PutMapping("/profile/my")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> updateMyProfile(
            @RequestBody Map<String, Object> updateRequest,
            HttpServletRequest request) {
        try {
            String jwt = parseJwt(request);
            if (jwt == null || !jwtUtils.validateJwtToken(jwt)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid token"));
            }

            Long doctorUserId = jwtUtils.getUserIdFromJwtToken(jwt);
            logger.info("Updating profile for doctor user ID: {}", doctorUserId);

            Optional<Doctor> doctorOpt = doctorService.findByUserId(doctorUserId);
            if (doctorOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Doctor profile not found"));
            }

            Long doctorId = doctorOpt.get().getId();

            // Extract update fields
            String bio = (String) updateRequest.get("bio");
            Integer experienceYears = updateRequest.get("experienceYears") != null ?
                    ((Number) updateRequest.get("experienceYears")).intValue() : null;
            BigDecimal consultationFee = updateRequest.get("consultationFee") != null ?
                    new BigDecimal(updateRequest.get("consultationFee").toString()) : null;
            String roomNumber = (String) updateRequest.get("roomNumber");

            Doctor updatedDoctor = doctorService.updateDoctorProfile(
                    doctorId, bio, experienceYears, consultationFee, roomNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Profile updated successfully");
            response.put("doctor", convertToDoctorDetailResponse(updatedDoctor));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error updating doctor profile: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Error: " + e.getMessage()));
        }
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }

    private Map<String, Object> convertToDoctorDetailResponse(Doctor doctor) {
        Map<String, Object> response = convertToDoctorResponse(doctor);
        response.put("bio", doctor.getBio()); // เอา bio เต็ม
        response.put("licenseNumber", doctor.getLicenseNumber());
        response.put("phone", doctor.getUser().getPhone());
        response.put("createdAt", doctor.getCreatedAt());

        return response;
    }

    private Map<String, Object> convertToSpecialtyResponse(Specialty specialty) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", specialty.getId());
        response.put("name", specialty.getName());
        response.put("description", specialty.getDescription());

        return response;
    }

    // Helper method for converting Doctor to response
    private Map<String, Object> convertToDoctorResponse(Doctor doctor) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", doctor.getId());
        response.put("doctorName", doctor.getDoctorName());
        response.put("email", doctor.getUser().getEmail());

        // Specialty info
        Map<String, Object> specialty = new HashMap<>();
        specialty.put("id", doctor.getSpecialty().getId());
        specialty.put("name", doctor.getSpecialty().getName());
        response.put("specialty", specialty);

        response.put("licenseNumber", doctor.getLicenseNumber());
        response.put("experienceYears", doctor.getExperienceYears());
        response.put("consultationFee", doctor.getConsultationFee());
        response.put("roomNumber", doctor.getRoomNumber());
        response.put("isActive", doctor.getIsActive());
        response.put("bio", doctor.getBio() != null ?
                (doctor.getBio().length() > 100 ?
                        doctor.getBio().substring(0, 100) + "..." :
                        doctor.getBio()) : null);

        return response;
    }

    /**
     * Smart doctor selection API - เลือกแพทย์ที่มีคิวน้อยที่สุด
     * GET /api/doctors/smart-select?specialty=...&date=YYYY-MM-DD
     */
    @GetMapping("/smart-select")
    public ResponseEntity<?> smartSelectDoctor(
            @RequestParam String specialty,
            @RequestParam(required = false) String date) {

        try {
            logger.info("🎯 Smart select doctor for specialty: {} on date: {}", specialty, date);

            // Get doctors by specialty
            List<Doctor> doctors = doctorService.findBySpecialtyName(specialty);

            if (doctors.isEmpty()) {
                logger.warn("⚠️ No doctors found for specialty: {}", specialty);
                Map<String, Object> response = new HashMap<>();
                response.put("message", "No doctors found for this specialty");
                response.put("doctor", null);
                return ResponseEntity.ok(response);
            }

            // Filter active doctors only
            doctors = doctors.stream()
                    .filter(Doctor::getIsActive)
                    .toList();

            if (doctors.isEmpty()) {
                logger.warn("⚠️ No active doctors found for specialty: {}", specialty);
                Map<String, Object> response = new HashMap<>();
                response.put("message", "No active doctors available for this specialty");
                response.put("doctor", null);
                return ResponseEntity.ok(response);
            }

            logger.info("✅ Found {} active doctors", doctors.size());

            // 🔍 Filter doctors by availability on the selected date
            if (date != null && !date.isEmpty()) {
                logger.info("🔍 Filtering doctors by availability on date: {}", date);

                List<Doctor> doctorsWithAvailability = doctors.stream()
                    .filter(doctor -> {
                        boolean hasAvailability = availabilityService.hasDoctorAvailabilityOnDate(
                            doctor.getId(),
                            date
                        );
                        if (!hasAvailability) {
                            logger.info("  ⊘ Doctor {} has NO availability on {}",
                                doctor.getDoctorName(), date);
                        }
                        return hasAvailability;
                    })
                    .toList();

                if (doctorsWithAvailability.isEmpty()) {
                    logger.warn("⚠️ No doctors available on {} for specialty: {}", date, specialty);
                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "No doctors have available time slots on this date. Please select another date.");
                    response.put("doctor", null);
                    response.put("totalDoctorsInSpecialty", doctors.size());
                    response.put("doctorsAvailableOnDate", 0);
                    return ResponseEntity.ok(response);
                }

                doctors = doctorsWithAvailability;
                logger.info("✅ {} doctors have availability on {}", doctors.size(), date);
            }

            // 🎯 Smart selection logic: เลือกแพทย์ที่มีคิวน้อยที่สุด
            Doctor selectedDoctor = null;

            if (date != null && !date.isEmpty()) {
                // กรณีมี date: เช็คคิวของแต่ละแพทย์ในวันนั้น
                logger.info("📊 Checking queue for each doctor on date: {}", date);

                Map<Doctor, Integer> doctorQueueMap = new HashMap<>();

                for (Doctor doctor : doctors) {
                    try {
                        List<Appointment> appointments = appointmentService.getAppointmentsByDoctorAndDate(
                            doctor.getId(),
                            date
                        );

                        // นับเฉพาะ PENDING และ CONFIRMED
                        long queueCount = appointments.stream()
                            .filter(apt -> apt.getStatus() == AppointmentStatus.PENDING ||
                                          apt.getStatus() == AppointmentStatus.CONFIRMED)
                            .count();

                        doctorQueueMap.put(doctor, (int) queueCount);
                        logger.info("  - {} (ID: {}): {} appointments",
                            doctor.getDoctorName(), doctor.getId(), queueCount);

                    } catch (Exception e) {
                        logger.warn("  - Error checking queue for doctor {}: {}",
                            doctor.getDoctorName(), e.getMessage());
                        doctorQueueMap.put(doctor, 0);
                    }
                }

                // หาแพทย์ที่มีคิวน้อยที่สุด
                int minQueue = doctorQueueMap.values().stream()
                    .min(Integer::compare)
                    .orElse(0);

                // กรณีมีหลายคนคิวเท่ากัน -> สุ่มเลือก
                List<Doctor> doctorsWithMinQueue = doctorQueueMap.entrySet().stream()
                    .filter(entry -> entry.getValue() == minQueue)
                    .map(Map.Entry::getKey)
                    .toList();

                logger.info("📌 {} doctors have minimum queue ({} appointments)",
                    doctorsWithMinQueue.size(), minQueue);

                Random random = new Random();
                selectedDoctor = doctorsWithMinQueue.get(random.nextInt(doctorsWithMinQueue.size()));

                logger.info("🎯 Selected doctor: {} (ID: {}) with {} appointments on {}",
                    selectedDoctor.getDoctorName(), selectedDoctor.getId(), minQueue, date);

            } else {
                // กรณีไม่มี date: สุ่มเลือก
                logger.info("🎲 No date provided, selecting randomly");
                Random random = new Random();
                selectedDoctor = doctors.get(random.nextInt(doctors.size()));
                logger.info("🎯 Selected doctor: {} (ID: {})",
                    selectedDoctor.getDoctorName(), selectedDoctor.getId());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Doctor selected successfully");
            response.put("doctor", convertToSimpleDoctorResponse(selectedDoctor));
            response.put("totalDoctorsInSpecialty", doctors.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Error in smart doctor selection:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to select doctor: " + e.getMessage()));
        }
    }

    // Helper method for simple doctor response (for lists)
    private Map<String, Object> convertToSimpleDoctorResponse(Doctor doctor) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", doctor.getId());
        response.put("doctorName", doctor.getDoctorName());
        response.put("email", doctor.getUser().getEmail());

        // Specialty info
        Map<String, Object> specialty = new HashMap<>();
        specialty.put("id", doctor.getSpecialty().getId());
        specialty.put("name", doctor.getSpecialty().getName());
        response.put("specialty", specialty);

        response.put("consultationFee", doctor.getConsultationFee());
        response.put("experienceYears", doctor.getExperienceYears());
        response.put("roomNumber", doctor.getRoomNumber());

        // Short bio for lists
        response.put("bio", doctor.getBio() != null ?
                (doctor.getBio().length() > 50 ?
                        doctor.getBio().substring(0, 50) + "..." :
                        doctor.getBio()) : null);

        return response;
    }
}