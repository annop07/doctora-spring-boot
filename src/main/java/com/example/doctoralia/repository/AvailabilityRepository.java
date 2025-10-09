package com.example.doctoralia.repository;

import com.example.doctoralia.model.Availability;
import com.example.doctoralia.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

    //หา availability ของหมอคนหนึ่ง
    List<Availability> findByDoctorAndIsActiveTrueOrderByDayOfWeekAscStartTimeAsc(Doctor doctor);

    //หา availability ของหมอคนหนึ่งในวันที่กำหนด
    List<Availability> findByDoctorAndDayOfWeekAndIsActiveTrueOrderByStartTimeAsc(Doctor doctor, Integer dayOfWeek);


    //หา availability ตาม doctor ID
    List<Availability> findByDoctorIdAndIsActiveTrueOrderByDayOfWeekAscStartTimeAsc(Long doctorId);


    //หา availability ตาม doctor ID และวัน
    List<Availability> findByDoctorIdAndDayOfWeekAndIsActiveTrueOrderByStartTimeAsc(Long doctorId, Integer dayOfWeek);

    //ตรวจสอบว่าหมอมี availability ในเวลาที่กำหนดหรือไม่
    @Query("SELECT a FROM Availability a WHERE " +
            "a.doctor.id = :doctorId AND " +
            "a.dayOfWeek = :dayOfWeek AND " +
            "a.startTime <= :time AND " +
            "a.endTime > :time AND " +
            "a.isActive = true")
    Optional<Availability> findDoctorAvailabilityAtTime(@Param("doctorId") Long doctorId,
                                                        @Param("dayOfWeek") Integer dayOfWeek,
                                                        @Param("time") LocalTime time);

    //ตรวจสอบเวลาซ้อนกันของหมอคนเดียว
    @Query("SELECT a FROM Availability a WHERE " +
            "a.doctor.id = :doctorId AND " +
            "a.dayOfWeek = :dayOfWeek AND " +
            "a.isActive = true AND " +
            "a.id <> :excludeId AND " +
            "((a.startTime <= :startTime AND a.endTime > :startTime) OR " +
            "(a.startTime < :endTime AND a.endTime >= :endTime) OR " +
            "(a.startTime >= :startTime AND a.endTime <= :endTime))")
    List<Availability> findOverlappingAvailabilities(@Param("doctorId") Long doctorId,
                                                     @Param("dayOfWeek") Integer dayOfWeek,
                                                     @Param("startTime") LocalTime startTime,
                                                     @Param("endTime") LocalTime endTime,
                                                     @Param("excludeId") Long excludeId);


    //หา availability ทั้งหมดของหมอที่่ active
    List<Availability> findByIsActiveTrueOrderByDoctorIdAscDayOfWeekAscStartTimeAsc();

    //นับจำนวน availability ของหมอ
    long countByDoctorAndIsActiveTrue(Doctor doctor);

    //ลบจำนวน availability ของหมอ
    void deleteByDoctorAndId(Doctor doctor, Long id);


    //หา availability ตาม ID และ doctor (สำหรับ security)
    Optional<Availability> findByIdAndDoctor(Long id, Doctor doctor);

    //หาแพทย์ทั้งหมดที่ว่างในช่วงเวลาที่กำหนด
    @Query("SELECT DISTINCT a.doctor FROM Availability a WHERE " +
            "a.dayOfWeek = :dayOfWeek AND " +
            "a.startTime <= :time AND " +
            "a.endTime > :time AND " +
            "a.isActive = true AND " +
            "a.doctor.isActive = true")
    List<Doctor> findDoctorsAvailableAtTime(@Param("dayOfWeek") Integer dayOfWeek,
                                            @Param("time") LocalTime time);

    //หาแพทย์ตาม specialty ที่ว่างในช่วงเวลาที่กำหนด
    @Query("SELECT DISTINCT a.doctor FROM Availability a WHERE " +
            "a.doctor.specialty.id = :specialtyId AND " +
            "a.dayOfWeek = :dayOfWeek AND " +
            "a.startTime <= :time AND " +
            "a.endTime > :time AND " +
            "a.isActive = true AND " +
            "a.doctor.isActive = true")
    List<Doctor> findDoctorsBySpecialtyAvailableAtTime(@Param("specialtyId") Long specialtyId,
                                                        @Param("dayOfWeek") Integer dayOfWeek,
                                                        @Param("time") LocalTime time);
}
