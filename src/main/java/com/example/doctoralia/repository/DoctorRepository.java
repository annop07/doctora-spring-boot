package com.example.doctoralia.repository;

import com.example.doctoralia.model.Doctor;
import com.example.doctoralia.model.Specialty;
import com.example.doctoralia.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    //หาหมอจาก User
    Optional<Doctor> findByUser(User user);

    //หาหมอจาก User ID
    Optional<Doctor> findByUserId(Long userId);

    //หาหมอจาก license number
    Optional<Doctor> findByLicenseNumber(String licenseNumber);

    //ดูว่ามี license นี้หรือยัง
    boolean existsByLicenseNumber(String licenseNumber);

    //หาหมอที่ active
    List<Doctor> findAllByIsActiveTrue();

    //หาหมอตาม Specialty (เฉพาะ active)
    List<Doctor> findBySpecialtyAndIsActiveTrue(Specialty specialty);

    /**
     * หาหมอตาม specialty ID (เฉพาะ active)
     */
    List<Doctor> findBySpecialtyIdAndIsActiveTrue(Long specialtyId);

    /**
     * หาหมอตาม specialty ID (รวม inactive) - สำหรับ admin
     */
    Page<Doctor> findBySpecialtyId(Long specialtyId, Pageable pageable);

    /**
     * ค้นหาหมอตามชื่อ (search ใน firstName และ lastName ของ User) - เฉพาะ active
     * Fixed version with proper null handling
     */
    @Query("SELECT d FROM Doctor d JOIN d.user u WHERE " +
            "d.isActive = true AND " +
            "(LOWER(COALESCE(u.firstName, '')) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
            "LOWER(COALESCE(u.lastName, '')) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
            "LOWER(CONCAT(COALESCE(u.firstName, ''), ' ', COALESCE(u.lastName, ''))) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<Doctor> findByDoctorNameContaining(@Param("name") String name);

    /**
     * ค้นหาหมอตามชื่อ (รวม inactive) - สำหรับ admin
     */
    @Query("SELECT d FROM Doctor d JOIN d.user u WHERE " +
            "(LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<Doctor> findByDoctorNameContainingIncludingInactive(@Param("name") String name);

    /**
     * Fixed query - ค้นหาหมอขั้นสูง (ชื่อ + แผนก + ค่าตรวจ) - เฉพาะ active
     * Fixed the CONCAT and LOWER functions for PostgreSQL
     */
    @Query(value = "SELECT d.* FROM doctors d " +
            "JOIN users u ON d.user_id = u.id " +
            "JOIN specialties s ON d.specialty_id = s.id WHERE " +
            "d.is_active = true AND " +
            "(:name IS NULL OR LOWER(COALESCE(u.first_name, '') || ' ' || COALESCE(u.last_name, '')) LIKE LOWER('%' || :name || '%')) AND " +
            "(:specialtyId IS NULL OR s.id = :specialtyId) AND " +
            "(:minFee IS NULL OR d.consultation_fee >= :minFee) AND " +
            "(:maxFee IS NULL OR d.consultation_fee <= :maxFee) " +
            "ORDER BY u.first_name",
            nativeQuery = true)
    Page<Doctor> findDoctorsWithFilters(@Param("name") String name,
                                        @Param("specialtyId") Long specialtyId,
                                        @Param("minFee") BigDecimal minFee,
                                        @Param("maxFee") BigDecimal maxFee,
                                        Pageable pageable);

    /**
     * Alternative simpler query if the above still has issues
     */
    @Query("SELECT d FROM Doctor d WHERE d.isActive = true AND " +
            "(:specialtyId IS NULL OR d.specialty.id = :specialtyId)")
    Page<Doctor> findDoctorsWithSpecialtyFilter(@Param("specialtyId") Long specialtyId, Pageable pageable);

    /**
     * ค้นหาหมอขั้นสูง (รวม inactive) - สำหรับ admin
     */
    @Query("SELECT d FROM Doctor d JOIN d.user u JOIN d.specialty s WHERE " +
            "(:name IS NULL OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:specialtyId IS NULL OR s.id = :specialtyId) AND " +
            "(:minFee IS NULL OR d.consultationFee >= :minFee) AND " +
            "(:maxFee IS NULL OR d.consultationFee <= :maxFee)")
    Page<Doctor> findDoctorsWithFiltersIncludingInactive(@Param("name") String name,
                                                         @Param("specialtyId") Long specialtyId,
                                                         @Param("minFee") BigDecimal minFee,
                                                         @Param("maxFee") BigDecimal maxFee,
                                                         Pageable pageable);

    /**
     * หาหมอตาม specialty พร้อม pagination (เฉพาะ active)
     */
    Page<Doctor> findBySpecialtyIdAndIsActiveTrue(Long specialtyId, Pageable pageable);

    /**
     * หาหมอทั้งหมดที่ active พร้อม pagination
     */
    Page<Doctor> findByIsActiveTrue(Pageable pageable);

    /**
     * นับจำนวนหมอใน specialty
     */
    long countBySpecialtyIdAndIsActiveTrue(Long specialtyId);

    /**
     * นับจำนวนหมอทั้งหมดที่ active
     */
    long countByIsActiveTrue();

    /**
     * หาหมอที่มีค่าตรวจในช่วงที่กำหนด
     */
    List<Doctor> findByConsultationFeeBetweenAndIsActiveTrue(BigDecimal minFee, BigDecimal maxFee);

    /**
     * หาหมอทั้งหมดที่ active เรียงตามชื่อ
     */
    @Query("SELECT d FROM Doctor d JOIN d.user u WHERE d.isActive = true ORDER BY u.firstName ASC")
    List<Doctor> findByIsActiveTrueOrderByDoctorNameAsc();

    /**
     * หาหมอตามชื่อ specialty และ active = true
     */
    @Query("SELECT d FROM Doctor d JOIN d.specialty s WHERE " +
            "d.isActive = true AND LOWER(s.name) = LOWER(:specialtyName)")
    List<Doctor> findBySpecialtyNameAndIsActiveTrue(@Param("specialtyName") String specialtyName);
}