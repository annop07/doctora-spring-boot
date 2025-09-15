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
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints (เฉพาะเจาะจงก่อน)
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/specialties", "/api/specialties/**").permitAll()
                        .requestMatchers("/api/doctors", "/api/doctors/search", "/api/doctors/specialty/**", "/api/doctors/stats").permitAll()
                        .requestMatchers("/api/doctors/{id:[0-9]+}").permitAll()  // เฉพาะ ID ที่เป็นตัวเลข
                        .requestMatchers("/api/public/**").permitAll()

                        // Protected endpoints (เฉพาะเจาะจงก่อนทั่วไป)
                        .requestMatchers("/api/doctors/me/**").authenticated()         // เฉพาะ /me
                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers("/api/appointments/**").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Default
                        .anyRequest().authenticated()
                )
                // เพิ่ม JWT filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        System.out.println("✅ SecurityConfig loaded successfully!");
        return http.build();
    }
}