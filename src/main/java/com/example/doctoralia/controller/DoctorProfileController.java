package com.example.doctoralia.controller;

import com.example.doctoralia.config.JwtUtils;
import com.example.doctoralia.dto.MessageResponse;
import com.example.doctoralia.dto.UpdateDoctorProfileRequest;
import com.example.doctoralia.model.Doctor;
import com.example.doctoralia.model.User;
import com.example.doctoralia.model.UserRole;
import com.example.doctoralia.service.DoctorService;
import com.example.doctoralia.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/doctors/me")
@CrossOrigin(origins = "*" , maxAge = 3600)
public class DoctorProfileController {
    private static Logger logger = LoggerFactory.getLogger(DoctorProfileController.class);

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    //หมอดูโปรไฟล์ตัวเอง
    @GetMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> getMyProfile(HttpServletRequest request) {
        try{
            String jwt = parseJwt(request);
            if (jwt == null || !jwtUtils.validateJwtToken(jwt)) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Invalid token"));
            }

            Long userId = jwtUtils.getUserIdFromJwtToken(jwt);
            String role = jwtUtils.getRoleFromJwtToken(jwt);

            //เช็ค role
            if (!UserRole.DOCTOR.name().equals(role)) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Access denied. Doctor role required."));
            }

            //หา doctor profile
            Optional<Doctor> doctorOpt = doctorService.findByUserId(userId);
            if (doctorOpt.isPresent()) {
                Doctor doctor = doctorOpt.get();
                return ResponseEntity.ok(convertToDoctorProfileResponse(doctor));
            } else {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Doctor profile not found."));
            }
        } catch (Exception e) {
            logger.error("Error getting doctor profile: ",e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    //อัพเดทโปรไฟล์หมอ
    @PutMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> updateMyProfile
            (@Valid @RequestBody UpdateDoctorProfileRequest request,
             HttpServletRequest httpRequest){

        try {
            String jwt = parseJwt(httpRequest);
            if (jwt == null || !jwtUtils.validateJwtToken(jwt)) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Invalid token"));
            }

            Long userId = jwtUtils.getUserIdFromJwtToken(jwt);
            String role = jwtUtils.getRoleFromJwtToken(jwt);

            //ตรวจสอบ role
            if (!UserRole.DOCTOR.name().equals(role)) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Access denied. Doctor role required."));
            }

            //หา doctor profile
            Optional<Doctor> doctorOpt = doctorService.findByUserId(userId);
            //เช็คว่ามีโปรไฟล์มั้ย
            if (doctorOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Doctor profile not found."));
            }

            Doctor doctor = doctorOpt.get();
            //อัพเดตข้อมูล Doctor
            Doctor updateDoctor = doctorService.updateDoctorProfile(
                    doctor.getId(),
                    request.getBio(),
                    request.getExperienceYears(),
                    request.getConsultationFee(),
                    request.getRoomNumber()
            );

            //อัพเดตข้อมูล user (ถ้ามี)
            if (request.getFirstName() != null || request.getLastName() != null ||request.getPhone() != null ) {
                userService.updateUser(
                        userId,
                        request.getFirstName() != null ? request.getFirstName() : doctor.getUser().getFirstName(),
                        request.getLastName() != null ? request.getLastName() : doctor.getUser().getLastName(),
                        request.getPhone() != null ? request.getPhone() : doctor.getUser().getPhone()
                );
            }

            logger.info("Doctor profile updated: {}", doctor.getLicenseNumber());

            // Reload doctor profile to get updated data
            Optional<Doctor> updatedDoctorOpt = doctorService.findByUserId(userId);
            if (updatedDoctorOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Profile updated successfully!");
                response.put("doctor", convertToDoctorProfileResponse(updatedDoctorOpt.get()));
                return ResponseEntity.ok(response);
            }

            return ResponseEntity.ok(new MessageResponse("Profile updated successfully!"));
        } catch (Exception e) {
            logger.error("Error updating doctor profile: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    //JWT Token เพื่อยืนยันตัวตน ดึงจาก ฤีะ้นพรผฟะรนื ้ำฟกำพ
    private String parseJwt(HttpServletRequest request) {
        String token = request.getHeader("Authorization");

        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return null;
    }

    //แปลง Doctor เป็น response format
    private Map<String, Object> convertToDoctorProfileResponse(Doctor doctor) {
        Map<String, Object> response = new HashMap<>();

        // Doctor info
        response.put("id", doctor.getId());
        response.put("licenseNumber", doctor.getLicenseNumber());
        response.put("bio", doctor.getBio());
        response.put("experienceYears", doctor.getExperienceYears());
        response.put("consultationFee", doctor.getConsultationFee());
        response.put("roomNumber", doctor.getRoomNumber());
        response.put("isActive", doctor.getIsActive());
        response.put("createdAt", doctor.getCreatedAt());
        response.put("updatedAt", doctor.getUpdatedAt());

        // User info
        User user = doctor.getUser();
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("email", user.getEmail());
        response.put("phone", user.getPhone());
        response.put("fullName", user.getFullName());

        // Specialty info
        Map<String, Object> specialty = new HashMap<>();
        specialty.put("id", doctor.getSpecialty().getId());
        specialty.put("name", doctor.getSpecialty().getName());
        specialty.put("description", doctor.getSpecialty().getDescription());
        response.put("specialty", specialty);

        return response;
    }
}
