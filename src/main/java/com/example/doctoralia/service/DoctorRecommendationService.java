package com.example.doctoralia.service;

import com.example.doctoralia.model.Doctor;
import com.example.doctoralia.repository.DoctorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DoctorRecommendationService {
    private static final Logger logger = LoggerFactory.getLogger(DoctorRecommendationService.class);

    @Autowired
    private DoctorRepository doctorRepository;


    /**
     * แนะนำแพทย์อัตโนมัติตามอาการและความต้องการ
     */
    public List<Doctor> recommendDoctors(Long specialtyId, String symptoms,
                                       BigDecimal maxFee, Integer maxExperienceYears,
                                       String preferredGender, Integer minRating) {

        logger.info("Recommending doctors for specialty: {}, symptoms: {}, maxFee: {}",
                   specialtyId, symptoms, maxFee);

        // ดึงแพทย์ในแผนกที่เลือก
        List<Doctor> candidateDoctors = specialtyId != null ?
            doctorRepository.findBySpecialtyIdAndIsActiveTrue(specialtyId) :
            doctorRepository.findAllByIsActiveTrue();

        if (candidateDoctors.isEmpty()) {
            logger.warn("No doctors found for specialty: {}", specialtyId);
            return new ArrayList<>();
        }

        // คำนวณคะแนนความเหมาะสมสำหรับแต่ละแพทย์
        List<DoctorScore> scoredDoctors = candidateDoctors.stream()
            .map(doctor -> new DoctorScore(doctor, calculateDoctorScore(doctor, symptoms, maxFee, maxExperienceYears, preferredGender, minRating)))
            .filter(ds -> ds.score > 0) // กรองเฉพาะที่มีคะแนน > 0
            .sorted((ds1, ds2) -> Double.compare(ds2.score, ds1.score)) // เรียงจากคะแนนสูงสุด
            .collect(Collectors.toList());

        logger.info("Found {} qualified doctors after scoring", scoredDoctors.size());

        // ส่งคืนรายการแพทย์ที่แนะนำ (สูงสุด 5 คน)
        return scoredDoctors.stream()
            .limit(5)
            .map(ds -> ds.doctor)
            .collect(Collectors.toList());
    }

    /**
     * คำนวณคะแนนความเหมาะสมของแพทย์
     */
    private double calculateDoctorScore(Doctor doctor, String symptoms,
                                      BigDecimal maxFee, Integer maxExperienceYears,
                                      String preferredGender, Integer minRating) {
        double score = 0.0;

        // คะแนนพื้นฐาน
        score += 10.0;

        // คะแนนจากประสบการณ์ (20%)
        if (doctor.getExperienceYears() != null) {
            double experienceScore = Math.min(doctor.getExperienceYears() * 2.0, 20.0);
            score += experienceScore;
        }

        // คะแนนจากค่าบริการ (15%)
        if (maxFee != null && doctor.getConsultationFee() != null) {
            if (doctor.getConsultationFee().compareTo(maxFee) <= 0) {
                // ยิ่งถูกกว่ายิ่งได้คะแนนสูง
                double feeRatio = doctor.getConsultationFee().doubleValue() / maxFee.doubleValue();
                score += (1.0 - feeRatio) * 15.0;
            } else {
                // ถ้าแพงเกินงบ ลดคะแนน
                score -= 10.0;
            }
        }

        // คะแนนจากเพศที่ต้องการ (10%)
        if (preferredGender != null && doctor.getUser() != null) {
            // สมมติว่าเราเพิ่มฟิลด์ gender ใน User model
            // score += doctor.getUser().getGender().equalsIgnoreCase(preferredGender) ? 10.0 : 0.0;
        }

        // คะแนนจากรีวิว/คะแนนผู้ป่วย (25%)
        // สมมติว่าเราจะเพิ่มระบบรีวิวในอนาคต
        // if (doctor.getAverageRating() != null) {
        //     score += doctor.getAverageRating() * 5.0; // คะแนน 1-5 คูณ 5
        // }

        // คะแนนจากความพร้อมใช้งาน (15%)
        if (doctor.getIsActive()) {
            score += 15.0;
        }

        // คะแนนจากการวิเคราะห์อาการ (15%)
        if (symptoms != null && !symptoms.trim().isEmpty()) {
            score += analyzeSymptomMatch(doctor, symptoms);
        }

        logger.debug("Doctor {} scored: {}", doctor.getUser().getFirstName(), score);
        return score;
    }

    /**
     * วิเคราะห์ความเหมาะสมของแพทย์กับอาการ
     */
    private double analyzeSymptomMatch(Doctor doctor, String symptoms) {
        if (doctor.getBio() == null || symptoms == null) {
            return 5.0; // คะแนนพื้นฐาน
        }

        String bio = doctor.getBio().toLowerCase();
        String symptomText = symptoms.toLowerCase();
        double matchScore = 0.0;

        // คำสำคัญที่เกี่ยวข้องกับแต่ละแผนก
        Map<String, List<String>> specialtyKeywords = createSpecialtyKeywords();

        String specialtyName = doctor.getSpecialty().getName().toLowerCase();
        List<String> keywords = specialtyKeywords.getOrDefault(specialtyName, new ArrayList<>());

        // ตรวจสอบคำสำคัญในอาการ
        for (String keyword : keywords) {
            if (symptomText.contains(keyword) || bio.contains(keyword)) {
                matchScore += 2.0;
            }
        }

        // จำกัดคะแนนสูงสุด
        return Math.min(matchScore, 15.0);
    }

    /**
     * สร้างคำสำคัญสำหรับแต่ละแผนก
     */
    private Map<String, List<String>> createSpecialtyKeywords() {
        Map<String, List<String>> keywords = new HashMap<>();

        keywords.put("อายุรกรรม", Arrays.asList("ไข้", "ปวดหัว", "ปวดท้อง", "ท้องเสีย", "ไอ", "เจ็บคอ", "เหนื่อย", "อ่อนเพลีย"));
        keywords.put("กุมารเวชกรรม", Arrays.asList("เด็ก", "ไข้", "วัคซีน", "เจริญเติบโต", "แพ้", "ผื่น"));
        keywords.put("ศัลยกรรม", Arrays.asList("บาดเจ็บ", "แผล", "ผ่าตัด", "เข่า", "มือ", "เท้า", "กระดูก"));
        keywords.put("ออร์โธปิดิกส์", Arrays.asList("กระดูก", "ข้อ", "เข่า", "ไหล่", "หลัง", "ปวด", "บาดเจ็บ"));
        keywords.put("หู คอ จมูก", Arrays.asList("หู", "คอ", "จมูก", "เจ็บคอ", "หูอื้อ", "เสียงแหบ", "จาม"));
        keywords.put("ตา", Arrays.asList("ตา", "มอง", "เบลอ", "ตาแห้ง", "ตาแดง", "ปวดตา"));
        keywords.put("สูตินรีเวช", Arrays.asList("ผู้หญิง", "ตั้งครรภ์", "คลอด", "ประจำเดือน", "มดลูก"));
        keywords.put("โรคหัวใจ", Arrays.asList("หัวใจ", "เต้น", "หายใจ", "เหนื่อย", "บวม", "ปวดหน้าอก"));
        keywords.put("ผิวหนัง", Arrays.asList("ผื่น", "คัน", "ผิว", "สิว", "แพ้", "แผล"));
        keywords.put("จิตเวช", Arrays.asList("เครียด", "ซึมเศร้า", "นอนไม่หลับ", "วิตกกังวล", "อารมณ์"));

        return keywords;
    }

    /**
     * Class สำหรับเก็บคะแนนของแพทย์
     */
    private static class DoctorScore {
        Doctor doctor;
        double score;

        DoctorScore(Doctor doctor, double score) {
            this.doctor = doctor;
            this.score = score;
        }
    }
}