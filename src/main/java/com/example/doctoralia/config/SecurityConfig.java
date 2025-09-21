package com.example.doctoralia.config;


import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
                        // Public endpoints (authentication not required)
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/specialties", "/api/specialties/**").permitAll()
                        .requestMatchers("/api/doctors", "/api/doctors/search", "/api/doctors/specialty/**", "/api/doctors/stats").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/doctors/{id:[0-9]+}").permitAll()  // Public doctor profiles
                        .requestMatchers("/api/public/**").permitAll()

                        // Protected endpoints (authentication required)
                        .requestMatchers("/api/doctors/me/**").authenticated()         // Doctor's own profile management
                        .requestMatchers("/api/users/**").authenticated()             // User profile management
                        .requestMatchers("/api/appointments/**").authenticated()      // Appointments

                        // Admin only endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Default - require authentication for everything else
                        .anyRequest().authenticated()
                )
                // Add JWT filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        System.out.println("✅ SecurityConfig loaded successfully!");

        return http.build();
    }
}