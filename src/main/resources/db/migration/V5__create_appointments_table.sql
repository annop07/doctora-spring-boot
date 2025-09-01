-- V5__create_appointments_table.sql
-- สร้างตารางการนัดหมาย (Simple approach ที่ใช้งานได้แน่นอน)

CREATE TYPE appointment_status AS ENUM ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW');

CREATE TABLE appointments (
                              id BIGSERIAL PRIMARY KEY,
                              doctor_id BIGINT NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
                              patient_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                              appointment_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
                              duration_minutes INTEGER DEFAULT 30,
                              status appointment_status DEFAULT 'PENDING',
                              notes TEXT, -- หมายเหตุจากคนไข้
                              doctor_notes TEXT, -- หมายเหตุจากหมอ
                              created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP

    -- Note: การป้องกันเวลาซ้อนกันจะทำใน Application Layer
    -- เพราะ PostgreSQL มีข้อจำกัดเรื่อง IMMUTABLE functions ใน EXCLUDE constraints
);

-- Index สำหรับการค้นหาและ performance
CREATE INDEX idx_appointments_doctor_id ON appointments(doctor_id);
CREATE INDEX idx_appointments_patient_id ON appointments(patient_id);
CREATE INDEX idx_appointments_datetime ON appointments(appointment_datetime);
CREATE INDEX idx_appointments_status ON appointments(status);
CREATE INDEX idx_appointments_doctor_datetime ON appointments(doctor_id, appointment_datetime);

-- Unique constraint: ป้องกันการนัดเวลาเดียวกันเป็นนาที (พื้นฐาน)
CREATE UNIQUE INDEX idx_appointments_exact_time
    ON appointments(doctor_id, appointment_datetime)
    WHERE status IN ('PENDING', 'CONFIRMED');

-- View สำหรับดูนัดหมายพร้อมข้อมูลที่เกี่ยวข้อง
CREATE VIEW appointment_details AS
SELECT
    a.id,
    a.appointment_datetime,
    a.duration_minutes,
    a.status,
    a.notes,
    a.doctor_notes,
    -- Doctor info
    d.room_number,
    doc_user.first_name || ' ' || doc_user.last_name AS doctor_name,
    s.name AS specialty_name,
    -- Patient info
    pat_user.first_name || ' ' || pat_user.last_name AS patient_name,
    pat_user.email AS patient_email,
    pat_user.phone AS patient_phone,
    a.created_at
FROM appointments a
         JOIN doctors d ON a.doctor_id = d.id
         JOIN users doc_user ON d.user_id = doc_user.id
         JOIN specialties s ON d.specialty_id = s.id
         JOIN users pat_user ON a.patient_id = pat_user.id
ORDER BY a.appointment_datetime;