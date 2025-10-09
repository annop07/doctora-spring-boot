package com.example.doctoralia.controller;

import com.example.doctoralia.config.JwtUtils;
import com.example.doctoralia.dto.MessageResponse;
import com.example.doctoralia.dto.UpdateProfileRequest;
import com.example.doctoralia.model.User;
import com.example.doctoralia.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
@RequestMapping("/api/users")
@CrossOrigin(origins = "*" , maxAge = 3600)
public class UserController {

    private static Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * ดึงข้อมูลโปรไฟล์ตัวเอง
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try{
            String jwt = parseJwt(request);

            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                String email = jwtUtils.getEmailFromJwtToken(jwt);

                Optional<User> userOpt = userService.findByEmail(email);

                if (userOpt.isPresent()) {
                    User user = userOpt.get();

                    Map<String, Object> response = new HashMap<>();
                    response.put("id", user.getId());
                    response.put("email", user.getEmail());
                    response.put("firstName", user.getFirstName());
                    response.put("lastName", user.getLastName());
                    response.put("fullName", user.getFullName());
                    response.put("role", user.getRole());
                    response.put("phone", user.getPhone());
                    response.put("createdAt", user.getCreatedAt());

                    return  ResponseEntity.ok(response);
                } else {
                    return ResponseEntity.badRequest()
                            .body(new MessageResponse("Error: User not found !"));

                }
            } else {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: User not found !"));
            }
        } catch (Exception e) {
            logger.error("Error getting current user: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * อัพเดทข้อมูลโปรไฟล์
     */
    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentUser(
            @RequestBody UpdateProfileRequest request,
            HttpServletRequest httpRequest) {
        try {
            // ดึง JWT Token จาก Authorization header
            String jwt = parseJwt(httpRequest);

            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                Long userId = jwtUtils.getUserIdFromJwtToken(jwt);

                // อัพเดทข้อมูล
                User updatedUser = userService.updateUser(
                        userId,
                        request.getFirstName(),
                        request.getLastName(),
                        request.getPhone()
                );

                logger.info("User profile updated: {}", updatedUser.getEmail());

                return ResponseEntity.ok(new MessageResponse("Profile updated successfully!"));

            } else {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Invalid token!"));
            }

        } catch (Exception e) {
            logger.error("Error updating user profile: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * ดึง JWT Token จาก Authorization header
     */
    private  String parseJwt(HttpServletRequest request){
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }

    /**
     * Get all users for admin (Admin only)
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsersForAdmin(HttpServletRequest request) {
        try {
            String jwt = parseJwt(request);
            if (jwt == null || !jwtUtils.validateJwtToken(jwt)) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Invalid token"));
            }

            String role = jwtUtils.getRoleFromJwtToken(jwt);
            if (!"ADMIN".equals(role)) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Access denied. Admin role required."));
            }

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
