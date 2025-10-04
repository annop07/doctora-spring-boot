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

    //หาหมอตาม Specialty
    List<Doctor> findBySpecialtyAndIsActiveTrue(Specialty specialty);

    /**
     * หาหมอตาม specialty ID
     */
    List<Doctor> findBySpecialtyIdAndIsActiveTrue(Long specialtyId);

    /**
     * ค้นหาหมอตามชื่อ (search ใน firstName และ lastName ของ User)
     */
    @Query("SELECT d FROM Doctor d JOIN d.user u WHERE " +
            "d.isActive = true AND " +
            "(LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<Doctor> findByDoctorNameContaining(@Param("name") String name);

    /**
     * ค้นหาหมอขั้นสูง (ชื่อ + แผนก + ค่าตรวจ)
     */
    @Query(value = "SELECT d.* FROM doctors d " +
            "JOIN users u ON u.id = d.user_id " +
            "JOIN specialties s ON s.id = d.specialty_id " +
            "WHERE d.is_active = true " +
            "AND (:name IS NULL OR (LOWER(u.first_name || ' ' || u.last_name) LIKE LOWER('%' || CAST(:name AS TEXT) || '%') " +
            "     OR LOWER(u.first_name) LIKE LOWER('%' || CAST(:name AS TEXT) || '%') " +
            "     OR LOWER(u.last_name) LIKE LOWER('%' || CAST(:name AS TEXT) || '%'))) " +
            "AND (:specialtyId IS NULL OR s.id = :specialtyId) " +
            "AND (:minFee IS NULL OR d.consultation_fee >= :minFee) " +
            "AND (:maxFee IS NULL OR d.consultation_fee <= :maxFee)",
            nativeQuery = true)
    Page<Doctor> findDoctorsWithFilters(@Param("name") String name,
                                        @Param("specialtyId") Long specialtyId,
                                        @Param("minFee") BigDecimal minFee,
                                        @Param("maxFee") BigDecimal maxFee,
                                        Pageable pageable);

    /**
     * หาหมอตาม specialty พร้อม pagination
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

    Long id(Long id);
}
