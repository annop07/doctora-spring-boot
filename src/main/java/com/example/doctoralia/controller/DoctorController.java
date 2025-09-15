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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/doctors/me")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DoctorController {

    private static Logger logger = LoggerFactory.getLogger(DoctorController.class);

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private SpecialtyService specialtyService;


    //ค้นหาหมอทั้งหมด
    @GetMapping
    public ResponseEntity<?> searchDoctors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long specialty,
            @RequestParam(required = false) BigDecimal minFee,
            @RequestParam(required = false) BigDecimal maxFee) {


        try {
            Page<Doctor> doctors;

            if (name != null || specialty != null || minFee != null || maxFee != null) {
                doctors = doctorService.searchDoctors(name, specialty, minFee, maxFee, page, size);
            } else {
                doctors = doctorService.getAllDoctors(page, size, sort);
            }
            // แปลงเป็น response format
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
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Error searching doctors: " + e.getMessage()));
        }
    }

    //ดูโปรไฟล์หมอ (Public api) ใครก็ดูได้
    @GetMapping("/{id}")
    public ResponseEntity<?> getDoctorById(@PathVariable Long id) {
        try {
            Optional<Doctor> doctorOpt = doctorService.findById(id);

            if (doctorOpt.isPresent()) {
                Doctor doctor = doctorOpt.get();

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

    //ดึงหมอตาม specialty (Public API)
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

    //ค้นหาตามชื่อ (public API)
    @GetMapping("/search")
    public ResponseEntity<?> searchDoctorsByName(@RequestParam String name) {
        try {
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

    // Helper methods สำหรับแปลง Entity เป็น Response
    private Map<String, Object> convertToDoctorResponse(Doctor doctor) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", doctor.getId());
        response.put("name", doctor.getDoctorName());
        response.put("firstName", doctor.getUser().getFirstName());
        response.put("lastName", doctor.getUser().getLastName());
        response.put("email", doctor.getUser().getEmail());
        response.put("specialty", convertToSpecialtyResponse(doctor.getSpecialty()));
        response.put("experienceYears", doctor.getExperienceYears());
        response.put("consultationFee", doctor.getConsultationFee());
        response.put("roomNumber", doctor.getRoomNumber());
        response.put("bio", doctor.getBio() != null ? doctor.getBio().substring(0, Math.min(100, doctor.getBio().length())) + "..." : null);

        return response;
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
}


