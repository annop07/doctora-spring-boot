package com.example.doctoralia.service;

import com.example.doctoralia.model.Availability;
import com.example.doctoralia.model.Doctor;
import com.example.doctoralia.repository.AvailabilityRepository;
import com.example.doctoralia.repository.DoctorRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class AvailabilityService {

    private static final Logger logger = LoggerFactory.getLogger(AvailabilityService.class);

    @Autowired
    private AvailabilityRepository availabilityRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    //เพิ่ม availability สำหรับหมอ
    public Availability addAvailability(Long doctorId, Integer dayOfWeek, LocalTime startTime, LocalTime endTime) {

        //หาหมอ
        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
        if (doctorOpt.isEmpty()) {
            throw new IllegalArgumentException("Doctor not found with ID: " +doctorId );
        }

        Doctor doctor = doctorOpt.get();

        // Validate input
        validateAvailabilityInput(dayOfWeek, startTime, endTime);


        // ตรวจสอบเวลาซ้อนกัน
        List<Availability> overlapping = availabilityRepository.findOverlappingAvailabilities(
                doctorId, dayOfWeek, startTime, endTime, 0L // 0L for new record
        );

        if (!overlapping.isEmpty()) {
            throw new IllegalArgumentException("Time slot overlaps with existing availability: " +
                    overlapping.get(0).getTimeRange());
        }

        //สร้าง availability
        Availability availability = new Availability(doctor,dayOfWeek,startTime,endTime);

        Availability saved = availabilityRepository.save(availability);
        logger.info("Availability added for doctor {}: {} {}", doctor.getDoctorName(),
                saved.getDayName(), saved.getTimeRange());
        return saved;

    }

    //แก้ไข availability
    public Availability updateAvailability(Long doctorId, Long availabilityId,
                                           Integer dayOfWeek, LocalTime startTime, LocalTime endTime) {

        //หาหมอ
        Optional<Doctor>  doctorOpt = doctorRepository.findById(doctorId);
        if (doctorOpt.isEmpty()) {
            throw new IllegalArgumentException("Doctor not found with ID: " +doctorId );
        }

        Doctor doctor = doctorOpt.get();

        //หา availability
        Optional<Availability> availabilityOpt = availabilityRepository.findById(availabilityId);
        if (availabilityOpt.isEmpty()) {
            throw new IllegalArgumentException("Availability not found or access denied");
        }

        Availability availability = availabilityOpt.get();

        // Validate input
        validateAvailabilityInput(dayOfWeek, startTime, endTime);

        // ตรวจสอบเวลาซ้อนกัน (ยกเว้นตัวเอง)
        List<Availability> overlapping = availabilityRepository.findOverlappingAvailabilities(
                doctorId, dayOfWeek, startTime, endTime, availabilityId
        );

        if (!overlapping.isEmpty()) {
            throw new IllegalArgumentException("Time slot overlaps with existing availability: " +
                    overlapping.get(0).getTimeRange());
        }

        // อัพเดท
        availability.setDayOfWeek(dayOfWeek);
        availability.setStartTime(startTime);
        availability.setEndTime(endTime);

        Availability updated = availabilityRepository.save(availability);
        logger.info("Availability updated for doctor {}: {} {}", doctor.getDoctorName(),
                updated.getDayName(), updated.getTimeRange());

        return updated;
    }

    //ลบ availability
    public void deleteAvailability(Long doctorId, Long availabilityId) {
        //หาหมอ
        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
        if (doctorOpt.isEmpty()) {
            throw new IllegalArgumentException("Doctor not found with ID: " +doctorId );
        }

        Doctor doctor = doctorOpt.get();

        //หา availability
        Optional<Availability> availabilityOpt = availabilityRepository.findByIdAndDoctor(availabilityId, doctor);
        if (availabilityOpt.isEmpty()) {
            throw new IllegalArgumentException("Availability not found or access denied");
        }

        Availability availability = availabilityOpt.get();

        // ทด ตรวจสอบว่ามี appointment ในชาวงเวลานี้หรือไม่

        availabilityRepository.delete(availability);
        logger.info("Availability deleted for doctor {}: {} {}", doctor.getDoctorName(),
                availability.getDayName(), availability.getTimeRange());

    }

    /**
     * ดึง availability ของหมอทั้งหมด
     */
    public List<Availability> getDoctorAvailabilities(Long doctorId) {
        return availabilityRepository.findByDoctorIdAndIsActiveTrueOrderByDayOfWeekAscStartTimeAsc(doctorId);
    }

    /**
     * ดึง availability ของหมอในวันที่กำหนด
     */
    public List<Availability> getDoctorAvailabilitiesByDay(Long doctorId, Integer dayOfWeek) {
        return availabilityRepository.findByDoctorIdAndDayOfWeekAndIsActiveTrueOrderByStartTimeAsc(doctorId, dayOfWeek);
    }

    /**
     * ตรวจสอบว่าหมอมี availability ในเวลาที่กำหนด
     */
    public boolean isDoctorAvailable(Long doctorId, Integer dayOfWeek, LocalTime time) {
        Optional<Availability> availability = availabilityRepository.findDoctorAvailabilityAtTime(
                doctorId, dayOfWeek, time
        );
        return availability.isPresent();
    }

    /**
     * หา availability ตาม ID
     */
    public Optional<Availability> findById(Long id) {
        return availabilityRepository.findById(id);
    }

    /**
     * Validate availability input
     */
    private void validateAvailabilityInput(Integer dayOfWeek, LocalTime startTime, LocalTime endTime) {
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new IllegalArgumentException("Day of week must be between 1-7");
        }

        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time are required");
        }

        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        // ตรวจสอบเวลาทำการปกติ (8:00 - 18:00)
        if (startTime.isBefore(LocalTime.of(6, 0)) || endTime.isAfter(LocalTime.of(22, 0))) {
            throw new IllegalArgumentException("Working hours must be between 06:00 - 22:00");
        }
    }
}
