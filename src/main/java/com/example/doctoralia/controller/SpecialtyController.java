package com.example.doctoralia.controller;

import com.example.doctoralia.dto.SpecialtyWithDoctorCount;
import com.example.doctoralia.model.Specialty;
import com.example.doctoralia.repository.SpecialtyRepository;
import com.example.doctoralia.service.SpecialtyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/specialties")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SpecialtyController {
    private static final Logger logger = LoggerFactory.getLogger(SpecialtyController.class);

    @Autowired
    private SpecialtyService specialtyService;

    //ดึงแผนกทั้งหมด
    @GetMapping
    public ResponseEntity<?> getAllSpecialties() {
        try {
            List<Specialty> specialties = specialtyService.getAllSpecialties();

            List<Map<String, Object>> response = specialties.stream()
                    .map(this::convertToSpecialtyResponse)
                    .toList();
            return ResponseEntity.ok(Map.of("specialties", response));
        } catch (Exception e) {
            logger.error("Error getting specialties with doctors: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Error getting specialties: " + e.getMessage()));
        }
    }

    //ดึงแผนกพร้อมจำนวนหมอ (public API)
    @GetMapping("/with-count")
    public ResponseEntity<?> getSpecialtiesWithDoctorCount() {
        try {
            List<SpecialtyWithDoctorCount> specialties =
                    specialtyService.getSpecialtiesWithDoctorCount();

            List<Map<String, Object>> response = specialties.stream()
                    .map(item -> {
                        Map<String, Object> specialty = convertToSpecialtyResponse(item.getSpecialty());
                        specialty.put("doctorCount", item.getDoctorCount());
                        return specialty;
                    })
                    .toList();

            return ResponseEntity.ok(Map.of("specialties", response));

        } catch (Exception e) {
            logger.error("Error getting specialties with doctor count: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Error getting specialties: " + e.getMessage()));
        }
    }

    //ดูแผนกตาม ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getSpecialtyById(@PathVariable Long id) {
        try {
            Optional<Specialty> specialtyOpt = specialtyService.findById(id);

            if (specialtyOpt.isPresent()) {
                Specialty specialty = specialtyOpt.get();
                long doctorCount = specialtyService.countDoctorsInSpecialty(id);

                Map<String, Object> response = convertToSpecialtyResponse(specialty);
                response.put("doctorCount", doctorCount);

                return ResponseEntity.ok(response);

            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Error getting specialty by ID: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Error getting specialty: " + e.getMessage()));
        }
    }

    //ค้นหาตามชื่อ (Public API)
    @GetMapping("/search")
    public ResponseEntity<?> searchSpecialties(@RequestParam String name) {
        try {
            List<Specialty> specialties = specialtyService.searchByName(name);

            List<Map<String, Object>> response = specialties.stream()
                    .map(this::convertToSpecialtyResponse)
                    .toList();

            return ResponseEntity.ok(Map.of("specialties", response));

        } catch (Exception e) {
            logger.error("Error searching specialties: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Error searching specialties: " + e.getMessage()));
        }
    }

    // Helper method สำหรับแปลง Entity เป็น Response
    private Map<String, Object> convertToSpecialtyResponse(Specialty specialty) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", specialty.getId());
        response.put("name", specialty.getName());
        response.put("description", specialty.getDescription());
        response.put("createdAt", specialty.getCreatedAt());

        return response;
    }

}
