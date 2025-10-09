package com.example.doctoralia.controller;

import com.example.doctoralia.dto.*;
import com.example.doctoralia.model.Doctor;
import com.example.doctoralia.model.Specialty;
import com.example.doctoralia.model.User;
import com.example.doctoralia.service.DoctorService;
import com.example.doctoralia.service.SpecialtyService;
import com.example.doctoralia.service.UserService;
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
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private SpecialtyService specialtyService;

    @Autowired
    private UserService userService;

    //สร้างหมอใหม่ ADMIN
    @PostMapping("/doctors")
    public ResponseEntity<?> addDoctor(@Valid @RequestBody CreateDoctorRequest request) {
        try {
            Doctor doctor = doctorService.createDoctor(
                    request.getUserId(),
                    request.getSpecialtyId(),
                    request.getLicenseNumber(),
                    request.getBio(),
                    request.getExperienceYears(),
                    request.getConsultationFee(),
                    request.getRoomNumber()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Doctor created successfully!");
            response.put("doctorId", doctor.getId());
            response.put("doctorName", doctor.getDoctorName());
            response.put("licenseNumber", doctor.getLicenseNumber());
            response.put("specialty", doctor.getSpecialtyName());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating doctor: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    //อัพเดตข้อมูลหมอ
    @PutMapping("/doctors/{id}")
    public ResponseEntity<?> updateDoctor(@PathVariable Long id,
                                         @Valid @RequestBody UpdateDoctorRequest request) {
        try {
            Doctor doctor = doctorService.updateDoctor(
                    id,
                    request.getSpecialtyId(),
                    request.getLicenseNumber(),
                    request.getBio(),
                    request.getExperienceYears(),
                    request.getConsultationFee(),
                    request.getRoomNumber()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Doctor updated successfully!");
            response.put("doctorId", doctor.getId());
            response.put("doctorName", doctor.getDoctorName());
            response.put("licenseNumber", doctor.getLicenseNumber());
            response.put("specialty", doctor.getSpecialtyName());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating doctor: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    //ลบหมอ
    @DeleteMapping("/doctors/{id}")
    public ResponseEntity<?> deleteDoctor(@PathVariable Long id) {
        try {
            doctorService.deleteDoctor(id);
            return ResponseEntity.ok(new MessageResponse("Doctor deleted successfully!"));
        } catch (Exception e) {
            logger.error("Error deleting doctor: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    //เปิด/ปิดการใช้งานหมอ
    @PutMapping("/doctors/{id}/status")
    public ResponseEntity<?> toggleDoctorStatus(@PathVariable Long id,
                                                @RequestBody ToggleStatusRequest request) {
        try {
            Doctor doctor = doctorService.toggleDoctorStatus(id, request.isActive());
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Doctor status updated successfully!");
            response.put("doctorName", doctor.getDoctorName());
            response.put("isActive", doctor.getIsActive());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error toggling doctor status: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }

    }

    //สร้างแผนกใหม่
    @PostMapping("/specialties")
    public ResponseEntity<?> createSpecialty(@Valid @RequestBody CreateSpecialtyRequest request){
        try {
            Specialty specialty = specialtyService.createSpecialty(
                    request.getName(),
                    request.getDescription()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Specialty created successfully!");
            response.put("Id", specialty.getId());
            response.put("Name", specialty.getName());
            response.put("description", specialty.getDescription());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating specialty: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    //อัพเดตแผนก
    @PutMapping("specialties/{id}")
    public ResponseEntity<?> updateSpecialty (@PathVariable Long id,
                                              @Valid @RequestBody CreateSpecialtyRequest request){
        try {
            Specialty specialty = specialtyService.updateSpecialty(
                    id,
                    request.getName(),
                    request.getDescription()
            );

            return ResponseEntity.ok(new MessageResponse("Specialty updated successfully!"));
        } catch (Exception e) {
            logger.error("Error updating specialty: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    //ลบแผนก
    @DeleteMapping("/specialties/{id}")
    public ResponseEntity<?> deleteSpecialty(@PathVariable Long id){
        try {
            specialtyService.deleteSpecialty(id);
            return ResponseEntity.ok(new MessageResponse("Deleted specialty successfully!"));
        }  catch (Exception e) {
            logger.error("Error deleting specialty: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    //ดูแผนกพร้อมจำนวนหมอ
    @GetMapping("/specialties/with-count")
    public ResponseEntity<?> getSpecialtyWithCount(){
        try {
            List<SpecialtyWithDoctorCount> specialties =
                    specialtyService.getSpecialtiesWithDoctorCount();
            return ResponseEntity.ok(Map.of("specialties", specialties));
        } catch (Exception e) {
            logger.error("Error getting specialties with count: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    //ดูสถิติระบบ
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(){
        try {
            DoctorStats doctorStats = doctorService.getDoctorStats();

            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("totalDoctors", doctorStats.getTotalDoctors());
            dashboard.put("totalSpecialties", doctorStats.getTotalSpecialties());

            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            logger.error("Error getting dashboard: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Get all users (Admin only) - for admin to see available doctor users
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();

            List<Map<String, Object>> response = users.stream()
                    .map(user -> {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("id", user.getId());
                        userMap.put("email", user.getEmail());
                        userMap.put("firstName", user.getFirstName());
                        userMap.put("lastName", user.getLastName());
                        userMap.put("role", user.getRole());
                        userMap.put("createdAt", user.getCreatedAt());
                        return userMap;
                    })
                    .toList();

            return ResponseEntity.ok(Map.of("users", response));

        } catch (Exception e) {
            logger.error("Error getting all users: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
}
