package com.example.doctoralia.repository;

import com.example.doctoralia.model.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {

    //ค้นหาจากชื่อแผนก
    Optional<Specialty> findByName(String name);

    //เช็คว่ามีแผนกนี้หรือยัง
    boolean existsByName(String name);

    //ค้นหาแผนกตามชื่อ
    @Query("SELECT s FROM Specialty s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Specialty> findByNameContaining(String name);

    //ดึงแผนกทั้งหมดเรียงตามชื่อ
    List<Specialty> findAllByOrderByNameAsc();

    //ดึงแผนกที่มีหมออยู่
    @Query("SELECT DISTINCT s FROM Specialty s WHERE s.id IN (SELECT d.specialty.id FROM Doctor d WHERE d.isActive = true)")
    List<Specialty> findSpecialtiesWithActiveDoctors();


}
