package com.example.doctoralia.controller;

import com.example.doctoralia.config.JwtUtils;
import com.example.doctoralia.dto.LoginRequest;
import com.example.doctoralia.dto.MessageResponse;
import com.example.doctoralia.dto.RegisterRequest;
import com.example.doctoralia.model.User;
import com.example.doctoralia.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*" , maxAge = 3600)
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * สมัครสมาชิก
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest request) {
        try{
            //เช็ค email ซ้ำ
            if (userService.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Email is already in use!"));
            }


            //สร้าง user ใหม่

            User user = userService.registerUser(
                    request.getEmail(),
                    request.getPassword(),
                    request.getFirstName(),

                    request.getLastName(),
                    request.getRole()

            );

            logger.info("User registered successfully: {}", request.getEmail());
            return ResponseEntity.ok(new MessageResponse("User registered successfully!"));

        } catch (Exception e){
            logger.error("Error registering user: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest request){
        try {
            Optional<User> userOpt = userService.getUserByEmail(
                    request.getEmail(),
                    request.getPassword()
            );

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                String jwt = jwtUtils.generateJwtToken(user);

                logger.info("User logged in successfully: {}", jwt);

                Map<String, Object> response = new HashMap<>();
                response.put("token", jwt);
                response.put("type", "Bearer");
                response.put("id", user.getId());
                response.put("email", user.getEmail());
                response.put("firstName", user.getFirstName());
                response.put("lastName", user.getLastName());
                response.put("role", user.getRole());

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Invalid email or password!"));
            }
        } catch (Exception e) {
            logger.error("Error login user: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }


    }

}

