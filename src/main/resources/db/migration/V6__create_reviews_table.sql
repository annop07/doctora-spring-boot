-- V6__create_reviews_table.sql
-- สร้างตารางรีวิวและคะแนน

CREATE TABLE reviews (
                         id BIGSERIAL PRIMARY KEY,
                         doctor_id BIGINT NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
                         patient_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                         appointment_id BIGINT REFERENCES appointments(id) ON DELETE SET NULL,
                         rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
                         comment TEXT,
                         is_anonymous BOOLEAN DEFAULT false,
                         created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Note: Business logic จะต้องตรวจสอบว่า patient มี role = 'PATIENT' ใน application layer

    -- Constraint: หนึ่งคนไข้รีวิวหมอคนเดียวได้แค่ครั้งเดียวต่อการนัด (ถ้ามี appointment_id)
                         UNIQUE(doctor_id, patient_id, appointment_id)
);

-- Index สำหรับการค้นหาและคำนวณคะแนนเฉลี่ย
CREATE INDEX idx_reviews_doctor_id ON reviews(doctor_id);
CREATE INDEX idx_reviews_patient_id ON reviews(patient_id);
CREATE INDEX idx_reviews_rating ON reviews(rating);
CREATE INDEX idx_reviews_created_at ON reviews(created_at);
CREATE INDEX idx_reviews_appointment_id ON reviews(appointment_id);

-- View สำหรับดูรีวิวพร้อมข้อมูลที่เกี่ยวข้อง
CREATE VIEW review_details AS
SELECT
    r.id,
    r.rating,
    r.comment,
    r.is_anonymous,
    r.created_at,
    -- Doctor info
    d.id as doctor_id,
    doc_user.first_name || ' ' || doc_user.last_name AS doctor_name,
    s.name AS specialty_name,
    d.room_number,
    -- Patient info (ถ้าไม่ anonymous)
    CASE
        WHEN r.is_anonymous = false
            THEN pat_user.first_name || ' ' || SUBSTRING(pat_user.last_name, 1, 1) || '.'
        ELSE 'Anonymous'
        END AS patient_name,
    -- Appointment info
    a.appointment_datetime
FROM reviews r
         JOIN doctors d ON r.doctor_id = d.id
         JOIN users doc_user ON d.user_id = doc_user.id
         JOIN specialties s ON d.specialty_id = s.id
         JOIN users pat_user ON r.patient_id = pat_user.id
         LEFT JOIN appointments a ON r.appointment_id = a.id
ORDER BY r.created_at DESC;

-- View สำหรับสถิติรีวิวของหมอแต่ละคน
CREATE VIEW doctor_review_stats AS
SELECT
    d.id as doctor_id,
    doc_user.first_name || ' ' || doc_user.last_name AS doctor_name,
    s.name AS specialty_name,
    d.room_number,
    d.consultation_fee,
    COUNT(r.id) AS total_reviews,
    COALESCE(ROUND(AVG(r.rating::numeric), 2), 0) AS average_rating,
    COUNT(CASE WHEN r.rating = 5 THEN 1 END) AS five_star_count,
    COUNT(CASE WHEN r.rating = 4 THEN 1 END) AS four_star_count,
    COUNT(CASE WHEN r.rating = 3 THEN 1 END) AS three_star_count,
    COUNT(CASE WHEN r.rating = 2 THEN 1 END) AS two_star_count,
    COUNT(CASE WHEN r.rating = 1 THEN 1 END) AS one_star_count,
    -- คำนวณเปอร์เซ็นต์รีวิวดี (4-5 ดาว)
    CASE
        WHEN COUNT(r.id) > 0
            THEN ROUND(
                (COUNT(CASE WHEN r.rating >= 4 THEN 1 END)::numeric / COUNT(r.id)::numeric) * 100,
                1
                 )
        ELSE 0
        END AS positive_percentage
FROM doctors d
         JOIN users doc_user ON d.user_id = doc_user.id
         JOIN specialties s ON d.specialty_id = s.id
         LEFT JOIN reviews r ON d.id = r.doctor_id
WHERE d.is_active = true
GROUP BY d.id, doc_user.first_name, doc_user.last_name, s.name, d.room_number, d.consultation_fee
ORDER BY average_rating DESC NULLS LAST, total_reviews DESC;