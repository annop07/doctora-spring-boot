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

    //ค้นหาหมอทั้งหมด (เฉพาะ active) - สำหรับ public use
    public Page<Doctor> getAllDoctors(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        return doctorRepository.findByIsActiveTrue(pageable);
    }

    //ค้นหาหมอทั้งหมด (รวม inactive) - สำหรับ admin
    public Page<Doctor> getAllDoctorsIncludingInactive(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        return doctorRepository.findAll(pageable);
    }

    /**
     * ค้นหาหมอขั้นสูง (ชื่อ + แผนก + ค่าตรวจ) - เฉพาะ active
     */
    public Page<Doctor> searchDoctors(String name, Long specialtyId, BigDecimal minFee, BigDecimal maxFee,
                                      int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("user.firstName").ascending());
        return doctorRepository.findDoctorsWithFilters(name, specialtyId, minFee, maxFee, pageable);
    }

    /**
     * ค้นหาหมอขั้นสูง (รวม inactive) - สำหรับ admin
     */
    public Page<Doctor> searchDoctorsIncludingInactive(String name, Long specialtyId, BigDecimal minFee, BigDecimal maxFee,
                                                       int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("user.firstName").ascending());
        return doctorRepository.findDoctorsWithFiltersIncludingInactive(name, specialtyId, minFee, maxFee, pageable);
    }

    //ค้นหาหมอตาม ID (รวม inactive) - สำหรับ admin
    public Optional<Doctor> findById(Long id) {
        return doctorRepository.findById(id);
    }

    //หาหมอจาก User ID
    public Optional<Doctor> findByUserId(Long userId) {
        return doctorRepository.findByUserId(userId);
    }

    //หาหมอจาก specialty (เฉพาะ active) - สำหรับ public
    public Page<Doctor> findBySpecialty(Long specialtyId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("user.firstName").ascending());
        return doctorRepository.findBySpecialtyIdAndIsActiveTrue(specialtyId, pageable);
    }

    //หาหมอจาก specialty (รวม inactive) - สำหรับ admin
    public Page<Doctor> findBySpecialtyIncludingInactive(Long specialtyId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("user.firstName").ascending());
        return doctorRepository.findBySpecialtyId(specialtyId, pageable);
    }

    //ค้นหาตามชื่อหมอ (เฉพาะ active) - สำหรับ public
    public List<Doctor> findByName(String name) {
        return doctorRepository.findByDoctorNameContaining(name);
    }

    //ค้นหาตามชื่อหมอ (รวม inactive) - สำหรับ admin
    public List<Doctor> findByNameIncludingInactive(String name) {
        return doctorRepository.findByDoctorNameContainingIncludingInactive(name);
    }

    //หาหมอทั้งหมดที่ active (สำหรับการแสดง list)
    public List<Doctor> findByIsActiveTrue() {
        return doctorRepository.findByIsActiveTrueOrderByDoctorNameAsc();
    }

    //หาหมอจากชื่อ specialty
    public List<Doctor> findBySpecialtyName(String specialtyName) {
        return doctorRepository.findBySpecialtyNameAndIsActiveTrue(specialtyName);
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
        // Allow any user role to be promoted to doctor (PATIENT, DOCTOR, etc.)
        // No role restriction - Admin can promote any user

        // Update user role to DOCTOR if it's not already
        if (!user.getRole().equals(UserRole.DOCTOR)) {
            logger.info("Promoting user {} from {} to DOCTOR role", user.getEmail(), user.getRole());
            user.setRole(UserRole.DOCTOR);
            userRepository.save(user);
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

    //อัพเดท doctor (สำหรับ Admin แก้ไขข้อมูลหมอ)
    public Doctor updateDoctor(Long doctorId, Long specialtyId, String licenseNumber,
                              String bio, Integer experienceYears, BigDecimal consultationFee, String roomNumber) {

        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
        if (doctorOpt.isEmpty()) {
            throw new IllegalArgumentException("Doctor not found with ID: " + doctorId);
        }
        Doctor doctor = doctorOpt.get();

        // ตรวจสอบ specialty มีอยู่
        if (specialtyId != null) {
            Optional<Specialty> specialtyOpt = specialtyRepository.findById(specialtyId);
            if (specialtyOpt.isEmpty()) {
                throw new IllegalArgumentException("Specialty not found with ID: " + specialtyId);
            }
            doctor.setSpecialty(specialtyOpt.get());
        }

        // ตรวจสอบ license number ซ้ำ (ถ้าเปลี่ยน)
        if (licenseNumber != null && !licenseNumber.equals(doctor.getLicenseNumber())) {
            if (doctorRepository.existsByLicenseNumber(licenseNumber)) {
                throw new IllegalArgumentException("License number already exists: " + licenseNumber);
            }
            doctor.setLicenseNumber(licenseNumber);
        }

        // อัพเดทข้อมูล
        if (bio != null) doctor.setBio(bio);
        if (experienceYears != null) doctor.setExperienceYears(experienceYears);
        if (consultationFee != null) doctor.setConsultationFee(consultationFee);
        if (roomNumber != null) doctor.setRoomNumber(roomNumber);

        Doctor updatedDoctor = doctorRepository.save(doctor);
        logger.info("Doctor updated by admin: {}", doctor.getLicenseNumber());

        return updatedDoctor;
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

    //ลบหมอ (สำหรับ Admin) - ลบทั้ง Doctor และ User
    @Transactional
    public void deleteDoctor(Long doctorId) {
        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
        if (doctorOpt.isEmpty()) {
            throw new IllegalArgumentException("Doctor not found with ID: " + doctorId);
        }

        Doctor doctor = doctorOpt.get();
        User user = doctor.getUser();
        String licenseNumber = doctor.getLicenseNumber();
        String userEmail = user.getEmail();
        Long userId = user.getId();

        // ลบ Doctor (Appointments ที่เชื่อมกับ Doctor จะถูกลบด้วย CASCADE)
        doctorRepository.deleteById(doctorId);
        doctorRepository.flush(); // บังคับให้ลบทันที

        // ลบ User หลังจากลบ Doctor แล้ว
        userRepository.deleteById(userId);
        userRepository.flush(); // บังคับให้ลบทันที

        logger.info("Doctor and User deleted by admin - License: {}, Email: {}", licenseNumber, userEmail);
    }

    //เปิด/ปิดการใช้งานหมอ (admin)
    public Doctor toggleDoctorStatus(Long doctorId, boolean isActive) {
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

        return new DoctorStats(totalDoctors, specialties.size());
    }
}