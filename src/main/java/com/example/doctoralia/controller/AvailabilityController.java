package com.example.doctoralia.controller;

import com.example.doctoralia.dto.MessageResponse;
import com.example.doctoralia.model.Availability;
import com.example.doctoralia.repository.AvailabilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/availabilities")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AvailabilityController {
    private static final Logger logger = LoggerFactory.getLogger(AvailabilityController.class);

    @Autowired
    private AvailabilityRepository availabilityRepository;

    /**
     * Get doctor's availability schedule
     * Returns time slots for each day of the week
     */
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<?> getDoctorAvailability(@PathVariable Long doctorId) {
        try {
            List<Availability> availabilities = availabilityRepository.findByDoctorIdOrderByDayAndTime(doctorId);

            if (availabilities.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "doctorId", doctorId,
                        "schedule", new ArrayList<>(),
                        "message", "No availability schedule found for this doctor"
                ));
            }

            // Group by day of week
            Map<Integer, List<Map<String, Object>>> scheduleByDay = new HashMap<>();

            for (Availability avail : availabilities) {
                int day = avail.getDayOfWeek();
                if (!scheduleByDay.containsKey(day)) {
                    scheduleByDay.put(day, new ArrayList<>());
                }

                Map<String, Object> slot = new HashMap<>();
                slot.put("id", avail.getId());
                slot.put("startTime", avail.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                slot.put("endTime", avail.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                slot.put("isActive", avail.getIsActive());

                scheduleByDay.get(day).add(slot);
            }

            // Convert to response format
            List<Map<String, Object>> schedule = new ArrayList<>();
            for (int day = 1; day <= 7; day++) {
                Map<String, Object> daySchedule = new HashMap<>();
                daySchedule.put("dayOfWeek", day);
                daySchedule.put("dayName", getDayName(day));
                daySchedule.put("slots", scheduleByDay.getOrDefault(day, new ArrayList<>()));
                schedule.add(daySchedule);
            }

            return ResponseEntity.ok(Map.of(
                    "doctorId", doctorId,
                    "schedule", schedule
            ));

        } catch (Exception e) {
            logger.error("Error getting doctor availability: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Get available time slots for a specific date
     */
    @GetMapping("/doctor/{doctorId}/date/{date}")
    public ResponseEntity<?> getAvailabilityForDate(
            @PathVariable Long doctorId,
            @PathVariable String date) {
        try {
            LocalDate requestedDate = LocalDate.parse(date);
            int dayOfWeek = requestedDate.getDayOfWeek().getValue(); // Monday=1, Sunday=7

            List<Availability> availabilities = availabilityRepository
                    .findByDoctorAndDayOfWeek(doctorId, dayOfWeek);

            // Generate 30-minute slots for each availability period
            List<Map<String, Object>> timeSlots = new ArrayList<>();

            for (Availability avail : availabilities) {
                LocalTime current = avail.getStartTime();
                LocalTime end = avail.getEndTime();

                while (current.plusMinutes(30).isBefore(end) || current.plusMinutes(30).equals(end)) {
                    Map<String, Object> slot = new HashMap<>();
                    slot.put("time", current.format(DateTimeFormatter.ofPattern("HH:mm")) +
                            "-" + current.plusMinutes(30).format(DateTimeFormatter.ofPattern("HH:mm")));
                    slot.put("available", true); // You can enhance this to check actual appointments
                    timeSlots.add(slot);

                    current = current.plusMinutes(30);
                }
            }

            return ResponseEntity.ok(Map.of(
                    "doctorId", doctorId,
                    "date", date,
                    "dayOfWeek", dayOfWeek,
                    "dayName", getDayName(dayOfWeek),
                    "timeSlots", timeSlots
            ));

        } catch (Exception e) {
            logger.error("Error getting availability for date: ", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    private String getDayName(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "Monday";
            case 2 -> "Tuesday";
            case 3 -> "Wednesday";
            case 4 -> "Thursday";
            case 5 -> "Friday";
            case 6 -> "Saturday";
            case 7 -> "Sunday";
            default -> "Unknown";
        };
    }
}