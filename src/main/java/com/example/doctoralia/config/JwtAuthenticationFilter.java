package com.example.doctoralia.config;

import com.example.doctoralia.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        logger.info("Processing request: {} {}", request.getMethod(), requestURI);
        
        try {
            // ดึง JWT token จาก Authorization header
            String jwt = parseJwt(request);
            
            if (jwt == null) {
                logger.warn("No JWT token found in request to: {}", requestURI);
            } else {
                logger.info("JWT token found, validating...");
                
                if (jwtUtils.validateJwtToken(jwt)) {
                    // ดึงข้อมูลจาก token
                    String email = jwtUtils.getEmailFromJwtToken(jwt);
                    String role = jwtUtils.getRoleFromJwtToken(jwt);

                    // สร้าง authorities สำหรับ Spring Security
                    List<SimpleGrantedAuthority> authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_" + role)
                    );

                    // สร้าง Authentication object
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(email, null, authorities);

                    // ตั้งค่า Security Context
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    logger.info("User authenticated: {} with role: {} for request: {}", email, role, requestURI);
                } else {
                    logger.error("JWT token validation failed for request: {}", requestURI);
                }
            }

        } catch (Exception e) {
            logger.error("Cannot set user authentication for request {}: ", requestURI, e);
        }

        // ส่งต่อไปยัง filter ถัดไป
        filterChain.doFilter(request, response);
    }

    /**
     * ดึง JWT token จาก Authorization header
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7); // ตัด "Bearer " ออก
        }

        return null;
    }
}
