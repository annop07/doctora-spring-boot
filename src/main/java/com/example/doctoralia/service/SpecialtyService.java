package com.example.doctoralia.service;

import com.example.doctoralia.dto.SpecialtyWithDoctorCount;
import com.example.doctoralia.model.Specialty;
import com.example.doctoralia.repository.DoctorRepository;
import com.example.doctoralia.repository.SpecialtyRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SpecialtyService {
    private static final Logger logger = LoggerFactory.getLogger(SpecialtyService.class);

    @Autowired
    private SpecialtyRepository specialtyRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    //ดึงแผนกทั้งหมด
    public List<Specialty> getAllSpecialties() {
        return specialtyRepository.findAllByOrderByNameAsc();
    }

    //ดึงแผนกที่มีหมออยู่
    public  List<Specialty> getSpecialtiesWithDoctors(){
        return specialtyRepository.findSpecialtiesWithActiveDoctors();
    }

    //หาแผนกตาม ID
    public Optional<Specialty> findById(Long id){
        return specialtyRepository.findById(id);
    }

    //หาแผนกตามชื่อ
    public Optional<Specialty> findByName(String name){
        return specialtyRepository.findByName(name);
    }

    //ค้นหาแผนกตามชื่อ (partial match)
    public List<Specialty> searchByName(String name) {
        return specialtyRepository.findByNameContaining(name);
    }

    //สร้างแผนกใหม่ (Admin)
    public Specialty createSpecialty(String name, String description) {
        if (specialtyRepository.existsByName(name)) {
            throw new IllegalArgumentException("Specialty already exists: " + name);
        }

        Specialty specialty = new Specialty();
        specialty.setName(name);
        specialty.setDescription(description);

        Specialty savedSpecialty = specialtyRepository.save(specialty);
        logger.info("Specialty created successfully: {}", name);

        return savedSpecialty;
    }

    /**
     * อัพเดทแผนก (สำหรับ admin)
     */
    public Specialty updateSpecialty(Long id, String name, String description) {
        Optional<Specialty> specialtyOpt = specialtyRepository.findById(id);
        if (specialtyOpt.isEmpty()) {
            throw new IllegalArgumentException("Specialty not found with ID: " + id);
        }

        Specialty specialty = specialtyOpt.get();

        // ตรวจสอบชื่อซ้ำ (ถ้าเปลี่ยนชื่อ)
        if (!specialty.getName().equals(name) && specialtyRepository.existsByName(name)) {
            throw new IllegalArgumentException("Specialty name already exists: " + name);
        }

        specialty.setName(name);
        specialty.setDescription(description);

        Specialty updatedSpecialty = specialtyRepository.save(specialty);
        logger.info("Specialty updated successfully: {}", name);

        return updatedSpecialty;
    }

    /**
     * ลบแผนก (สำหรับ admin) - ต้องไม่มีหมออยู่
     */
    public void deleteSpecialty(Long id) {
        Optional<Specialty> specialtyOpt = specialtyRepository.findById(id);
        if (specialtyOpt.isEmpty()) {
            throw new IllegalArgumentException("Specialty not found with ID: " + id);
        }

        // ตรวจสอบว่ามีหมออยู่ในแผนกนี้หรือไม่
        long doctorCount = doctorRepository.countBySpecialtyIdAndIsActiveTrue(id);
        if (doctorCount > 0) {
            throw new IllegalArgumentException("Cannot delete specialty. There are " + doctorCount + " active doctors in this specialty.");
        }

        specialtyRepository.deleteById(id);
        logger.info("Specialty deleted successfully: {}", specialtyOpt.get().getName());
    }

    /**
     * นับจำนวนหมอในแผนก
     */
    public long countDoctorsInSpecialty(Long specialtyId) {
        return doctorRepository.countBySpecialtyIdAndIsActiveTrue(specialtyId);
    }

    /**
     * ดึงแผนกพร้อมจำนวนหมอ
     */
    public List<SpecialtyWithDoctorCount> getSpecialtiesWithDoctorCount() {
        List<Specialty> specialties = specialtyRepository.findAllByOrderByNameAsc();

        return specialties.stream()
                .map(specialty -> {
                    long doctorCount = doctorRepository.countBySpecialtyIdAndIsActiveTrue(specialty.getId());
                    return new SpecialtyWithDoctorCount(specialty, doctorCount);
                })
                .toList();
    }
}
