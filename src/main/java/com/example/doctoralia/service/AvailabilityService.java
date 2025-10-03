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

    @Autowired
    private DoctorService doctorService;

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
        logger.info("Attempting to delete availability ID: {} for doctor ID: {}", availabilityId, doctorId);

        //หาหมอ
        Optional<Doctor> doctorOpt = doctorService.findById(doctorId);
        if (doctorOpt.isEmpty()) {
            logger.error("Doctor not found with ID: {}", doctorId);
            throw new IllegalArgumentException("Doctor not found with ID: " +doctorId );
        }

        Doctor doctor = doctorOpt.get();
        logger.info("Doctor found: {}", doctor.getDoctorName());

        //หา availability
        Optional<Availability> availabilityOpt = availabilityRepository.findByIdAndDoctor(availabilityId, doctor);
        logger.info("Finding availability with ID: {} for doctor: {}", availabilityId, doctor.getId());

        if (availabilityOpt.isEmpty()) {
            logger.error("Availability not found - ID: {}, Doctor ID: {}", availabilityId, doctor.getId());

            // Debug: Check if availability exists at all
            Optional<Availability> anyAvailability = availabilityRepository.findById(availabilityId);
            if (anyAvailability.isPresent()) {
                logger.error("Availability exists but belongs to doctor ID: {}", anyAvailability.get().getDoctor().getId());
            } else {
                logger.error("Availability with ID {} does not exist in database", availabilityId);
            }

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
     * เช็คว่าแพทย์มี availability ในวันที่กำหนดหรือไม่
     * @param doctorId ID ของแพทย์
     * @param date วันที่ในรูปแบบ String "YYYY-MM-DD"
     * @return true ถ้ามี availability, false ถ้าไม่มี
     */
    public boolean hasDoctorAvailabilityOnDate(Long doctorId, String date) {
        try {
            if (date == null || date.isEmpty()) {
                return false;
            }

            // แปลงวันที่เป็น dayOfWeek (1=Monday, 7=Sunday)
            java.time.LocalDate localDate = java.time.LocalDate.parse(date);
            int dayOfWeek = localDate.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday

            logger.info("Checking availability for doctor {} on {} (dayOfWeek: {})",
                doctorId, date, dayOfWeek);

            // ค้นหา availability ของแพทย์ในวันนั้น
            List<Availability> availabilities = availabilityRepository
                .findByDoctorIdAndDayOfWeekAndIsActiveTrueOrderByStartTimeAsc(doctorId, dayOfWeek);

            boolean hasAvailability = !availabilities.isEmpty();

            if (hasAvailability) {
                logger.info("✅ Doctor {} has {} availability slot(s) on {}",
                    doctorId, availabilities.size(), date);
            } else {
                logger.info("❌ Doctor {} has NO availability on {}", doctorId, date);
            }

            return hasAvailability;

        } catch (Exception e) {
            logger.error("Error checking availability for doctor {} on {}: {}",
                doctorId, date, e.getMessage());
            return false;
        }
    }

    /**
     * ดึง availability ของแพทย์ในวันที่กำหนด (จาก date string)
     * @param doctorId ID ของแพทย์
     * @param date วันที่ในรูปแบบ "YYYY-MM-DD"
     * @return List ของ Availability
     */
    public List<Availability> getDoctorAvailabilitiesByDate(Long doctorId, String date) {
        try {
            if (date == null || date.isEmpty()) {
                return List.of();
            }

            java.time.LocalDate localDate = java.time.LocalDate.parse(date);
            int dayOfWeek = localDate.getDayOfWeek().getValue();

            return availabilityRepository
                .findByDoctorIdAndDayOfWeekAndIsActiveTrueOrderByStartTimeAsc(doctorId, dayOfWeek);

        } catch (Exception e) {
            logger.error("Error getting availabilities for doctor {} on {}: {}",
                doctorId, date, e.getMessage());
            return List.of();
        }
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
