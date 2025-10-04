package com.example.doctoralia.service;

import com.example.doctoralia.dto.DoctorStats;
import com.example.doctoralia.model.Doctor;
import com.example.doctoralia.model.Specialty;
import com.example.doctoralia.model.User;
import com.example.doctoralia.model.UserRole;
import com.example.doctoralia.repository.DoctorRepository;
import com.example.doctoralia.repository.SpecialtyRepository;
import com.example.doctoralia.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional()
public class DoctorService {
    private static final Logger logger = LoggerFactory.getLogger(DoctorService.class);

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private SpecialtyRepository specialtyRepository;

    @Autowired
    private UserRepository userRepository;

    //ค้นหาหมอทั้งหมด
    public Page<Doctor> getAllDoctors(int page, int size, String sortBy) {
        Sort sort = parseSortParameter(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return doctorRepository.findByIsActiveTrue(pageable);
    }

    /**
     * ค้นหาหมอขั้นสูง (ชื่อ + แผนก + ค่าตรวจ)
     */
    public Page<Doctor> searchDoctors(String name, Long specialtyId, BigDecimal minFee, BigDecimal maxFee,
                                      int page, int size, String sortBy) {
        Sort sort = parseSortParameter(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return doctorRepository.findDoctorsWithFilters(name, specialtyId, minFee, maxFee, pageable);
    }

    /**
     * Parse sort parameter (e.g., "id,desc" -> Sort.by(Direction.DESC, "id"))
     */
    private Sort parseSortParameter(String sortBy) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return Sort.by("id");
        }

        String[] parts = sortBy.split(",");
        String property = parts[0].trim();

        // Map frontend property names to actual entity properties
        switch (property) {
            case "name":
                property = "user.firstName"; // Sort by user's first name
                break;
            case "experienceYears":
                property = "experienceYears";
                break;
            case "consultationFee":
                property = "consultationFee";
                break;
            case "id":
            default:
                property = "id";
                break;
        }

        if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())) {
            return Sort.by(Sort.Direction.DESC, property);
        } else {
            return Sort.by(Sort.Direction.ASC, property);
        }
    }

    //ค้นหาหมอตาม ID
    public Optional<Doctor> findById(Long id) {
        return doctorRepository.findById(id);
    }

    //หาหมอจาก User ID
    public Optional<Doctor> findByUserId(Long userId) {
        return doctorRepository.findByUserId(userId);
    }

    //หาหมอจาก specialty
    public Page<Doctor> findBySpecialty(Long specialtyId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return doctorRepository.findBySpecialtyIdAndIsActiveTrue(specialtyId, pageable);
    }

    //ค้นหาตามชื่อหมอ
    public List<Doctor> findByName(String name) {
        return doctorRepository.findByDoctorNameContaining(name);
    }

    /**
     * สำหรับAdmin
     * สร้างหมอ
     */

    public Doctor createDoctor(
            Long userId, Long specialtyId, String licenseNumber,
            String bio, Integer experienceYears, BigDecimal consultationFee, String roomNumber) {

        //เช็ค user ที่มีอยู่ว่าเป็น role doctor
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }

        User user = userOpt.get();
        if (!user.getRole().equals(UserRole.DOCTOR)) {
            throw new IllegalArgumentException("User must have DOCTOR role");
        }

        //เช็ค user ยังไม่มี doctor profile
        if ((doctorRepository.findByUserId(userId).isPresent())) {
            throw new IllegalArgumentException("Doctor profile already exists for this user");
        }

        // ตรวจสอบ specialty มีอยู่
        Optional<Specialty> specialtyOpt = specialtyRepository.findById(specialtyId);
        if (specialtyOpt.isEmpty()) {
            throw new IllegalArgumentException("Specialty not found with ID: " + specialtyId);
        }

        // ตรวจสอบ license number ซ้ำ
        if (doctorRepository.existsByLicenseNumber(licenseNumber)) {
            throw new IllegalArgumentException("License number already exists: " + licenseNumber);
        }

        // สร้าง doctor ใหม่
        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setSpecialty(specialtyOpt.get());
        doctor.setLicenseNumber(licenseNumber);
        doctor.setBio(bio);
        doctor.setExperienceYears(experienceYears);
        doctor.setConsultationFee(consultationFee);
        doctor.setRoomNumber(roomNumber);
        doctor.setIsActive(true);

        Doctor savedDoctor = doctorRepository.save(doctor);
        logger.info("Doctor created successfully: {} for user: {}", licenseNumber, user.getEmail());

        return savedDoctor;

    }

    //อัพเดท doctor profile (สำหรับหมอแก้ไขตัวเอง)
    public Doctor updateDoctorProfile(Long doctorId, String bio, Integer experienceYears,
                                      BigDecimal consultationFee, String roomNumber) {

        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
        if (doctorOpt.isEmpty()) {
            throw new IllegalArgumentException("Doctor not found with ID: " + doctorId);
        }
        Doctor doctor = doctorOpt.get();

        // อัพเดทข้อมูล
        if (bio != null) doctor.setBio(bio);
        if (experienceYears != null) doctor.setExperienceYears(experienceYears);
        if (consultationFee != null) doctor.setConsultationFee(consultationFee);
        if (roomNumber != null) doctor.setRoomNumber(roomNumber);

        Doctor updatedDoctor = doctorRepository.save(doctor);
        logger.info("Doctor profile updated: {}", doctor.getLicenseNumber());

        return updatedDoctor;
    }

    //เปิด/ปิดการใช้งานหมอ (admin)
    public Doctor toggleDoctorStatus(Long doctorId,boolean isActive) {
        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
        if (doctorOpt.isEmpty()) {
            throw new IllegalArgumentException("Doctor not found with ID: " + doctorId);
        }

        Doctor doctor = doctorOpt.get();
        doctor.setIsActive(isActive);

        Doctor updatedDoctor = doctorRepository.save(doctor);
        logger.info("Doctor status updated: {} - Active: {}", doctor.getLicenseNumber(), isActive);

        return updatedDoctor;
    }

    //ดึงสถิติหมอ
    public DoctorStats getDoctorStats(){
        long totalDoctors = doctorRepository.countByIsActiveTrue();
        List<Specialty> specialties = specialtyRepository.findSpecialtiesWithActiveDoctors();

        return new DoctorStats(totalDoctors,specialties.size());
    }


}
