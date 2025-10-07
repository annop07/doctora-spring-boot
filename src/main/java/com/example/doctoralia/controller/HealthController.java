package com.example.doctoralia.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", maxAge = 3600)
public class HealthController {

    /**
     * Simple health check endpoint
     * ใช้สำหรับ Railway healthcheck
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Application is running");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * Readiness check - ตรวจสอบว่า database พร้อมหรือไม่
     */
    @GetMapping("/ready")
    public ResponseEntity<?> ready() {
        // ถ้า endpoint นี้ทำงานได้ แสดงว่า Spring Boot เริ่มต้นสำเร็จ
        Map<String, Object> response = new HashMap<>();
        response.put("status", "READY");
        response.put("message", "Application is ready to serve requests");
        return ResponseEntity.ok(response);
    }
}