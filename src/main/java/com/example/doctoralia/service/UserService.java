package com.example.doctoralia.service;

import com.example.doctoralia.config.JwtUtils;
import com.example.doctoralia.model.User;
import com.example.doctoralia.model.UserRole;
import com.example.doctoralia.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    //สมัครสมาชิก
    public User registerUser(String email, String password, String firstName, String lastName, UserRole role) {
        //ดูอีเมลซ้ำ
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        //สร้าง user ใหม่
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);

        User savedUser = userRepository.save(user);
        logger.info("User registered successfully: {} with role {}",email,role);

        return savedUser;
    }

    /**
     * เช็ค email และ password สำหรับ login
     */
    public Optional<User> getUserByEmail(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            //เช็ค password
            if (passwordEncoder.matches(password, user.getPassword())) {
                logger.info("User found successfully: {} with role {}",email,user.getRole());
                return Optional.of(user);
            } else {
                logger.warn("Invalid password attempt for email: {}",email);
            }
        } else {
            logger.warn("User not found for email: {}",email);
        }

        return Optional.empty();
    }

    /**
     * หา user จาก email
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * หา user จาก ID
     */
    public Optional<User> findById(Long id){
        return userRepository.findById(id);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * อัพเดท user profile
     */
    public User updateUser(Long userId,String firstName,String lastname,String phone){
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setFirstName(firstName);
            user.setLastName(lastname);
            user.setPhone(phone);

            User updatedUser = userRepository.save(user);
            logger.info("User profile updated: {}", user.getEmail());

            return updatedUser;
        } else {
            throw new IllegalArgumentException("User not found for id: " + userId);
        }
    }

    /**
     * เปลี่ยน password
     */
    public void changePassword(Long userId,String oldPassword,String newPassword){
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            //เช็ค password เดิมว่าถูกมั้ย
            if (passwordEncoder.matches(oldPassword, user.getPassword())) {
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                logger.info("Password changed successfully for user: {}", user.getEmail());
            } else {
                throw new IllegalArgumentException("Invalid old password: " + oldPassword);
            }
        } else {
            throw new IllegalArgumentException("User not found for id: " + userId);
        }
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
