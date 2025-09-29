package com.example.doctoralia.controller;

import com.example.doctoralia.dto.DoctorStats;
import com.example.doctoralia.model.Doctor;
import com.example.doctoralia.model.Specialty;
import com.example.doctoralia.repository.DoctorRepository;
import com.example.doctoralia.repository.SpecialtyRepository;
import com.example.doctoralia.service.DoctorService;
import com.example.doctoralia.service.SpecialtyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/doctors")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DoctorController {

    private static Logger logger = LoggerFactory.getLogger(DoctorController.class);

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private SpecialtyService specialtyService;

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