package com.example.doctoralia.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        System.out.println("🚀 SecurityConfig is loading...");

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints (เฉพาะเจาะจงก่อน)
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/health").permitAll()  // Health check
                        .requestMatchers("/api/specialties", "/api/specialties/**").permitAll()
                        .requestMatchers("/api/doctors", "/api/doctors/search", "/api/doctors/specialty/**", "/api/doctors/stats").permitAll()
                        .requestMatchers("/api/doctors/{id:[0-9]+}").permitAll()  // เฉพาะ ID ตัวเลข
                        .requestMatchers("/api/doctors/me", "/api/doctors/me/**").permitAll()  // Public doctor endpoints
                        .requestMatchers("/api/availability/doctor/**").permitAll() // ดูตารางเวลาหมอ (public)
                        .requestMatchers("/api/public/**").permitAll()

                        // Protected endpoints - PATIENT
                        .requestMatchers("/api/appointments").hasRole("PATIENT")           // POST: จองนัด
                        .requestMatchers("/api/appointments/my").hasRole("PATIENT")        // GET: ดูนัดตัวเอง
                        .requestMatchers("/api/appointments/*/cancel").hasRole("PATIENT")  // PUT: ยกเลิกนัด

                        // Protected endpoints - DOCTOR (เฉพาะ profile management)
                        .requestMatchers("/api/doctors/profile/**").hasRole("DOCTOR")         // จัดการโปรไฟล์หมอ
                        .requestMatchers("/api/availability").hasRole("DOCTOR")               // จัดการตารางเวลา (POST)
                        .requestMatchers("/api/availability/my").hasRole("DOCTOR")            // ดูตารางเวลาตัวเอง
                        .requestMatchers("/api/availability/*").hasRole("DOCTOR")             // จัดการตารางเวลา (PUT/DELETE)
                        .requestMatchers("/api/appointments/my-patients").hasRole("DOCTOR")   // ดูนัดคนไข้
                        .requestMatchers("/api/appointments/*/confirm").hasRole("DOCTOR")     // อนุมัตินัด
                        .requestMatchers("/api/appointments/*/reject").hasRole("DOCTOR")      // ปฏิเสธนัด

                        // Protected endpoints - ALL AUTHENTICATED
                        .requestMatchers("/api/users/**").authenticated()

                        // Admin only
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Default
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        System.out.println("✅ SecurityConfig loaded successfully!");
        return http.build();
    }
}