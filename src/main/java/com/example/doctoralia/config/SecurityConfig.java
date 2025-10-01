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

    // In SecurityConfig.java, add to the public endpoints section:
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints (authentication not required)
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/specialties", "/api/specialties/**").permitAll()
                        .requestMatchers("/api/doctors", "/api/doctors/search", "/api/doctors/specialty/**", "/api/doctors/stats", "/api/doctors/active", "/api/doctors/by-specialty").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/doctors/{id:[0-9]+}").permitAll()

                        // ⭐ ADD THIS LINE - Make availability endpoints public
                        .requestMatchers("/api/availabilities/**").permitAll()

                        .requestMatchers("/api/public/**").permitAll()

                        // Protected endpoints (authentication required)
                        .requestMatchers("/api/doctors/me/**").authenticated()
                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers("/api/appointments/**").authenticated()

                        // Admin only endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Default - require authentication for everything else
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}