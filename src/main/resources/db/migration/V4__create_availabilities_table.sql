-- V4__create_availabilities_table.sql
-- สร้างตารางเวลาว่างของหมอ พร้อม EXCLUDE constraint

-- เปิดใช้งาน btree_gist extension สำหรับ EXCLUDE constraint
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE availabilities (
                                id BIGSERIAL PRIMARY KEY,
                                doctor_id BIGINT NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
                                day_of_week INTEGER NOT NULL CHECK (day_of_week >= 1 AND day_of_week <= 7), -- 1=Monday, 7=Sunday
                                start_time TIME NOT NULL,
                                end_time TIME NOT NULL,
                                is_active BOOLEAN DEFAULT true,
                                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Constraint: เวลาเริ่มต้องน้อยกว่าเวลาสิ้นสุด
                                CONSTRAINT check_time_range CHECK (start_time < end_time),

    -- EXCLUDE constraint: ป้องกันเวลาซ้อนกันในวันเดียวกัน (ใช้ btree_gist)
                                EXCLUDE USING GIST (
        doctor_id WITH =,
        day_of_week WITH =,
        tsrange(
            ('1970-01-01'::date + start_time)::timestamp,
            ('1970-01-01'::date + end_time)::timestamp,
            '[)'
        ) WITH &&
    ) WHERE (is_active = true)
);

-- Index สำหรับการค้นหา
CREATE INDEX idx_availabilities_doctor_id ON availabilities(doctor_id);
CREATE INDEX idx_availabilities_day_active ON availabilities(day_of_week, is_active);

-- Helper view สำหรับแสดงชื่อวัน
CREATE VIEW availability_view AS
SELECT
    a.id,
    a.doctor_id,
    d.room_number,
    u.first_name || ' ' || u.last_name AS doctor_name,
    s.name AS specialty_name,
    CASE a.day_of_week
        WHEN 1 THEN 'Monday'
        WHEN 2 THEN 'Tuesday'
        WHEN 3 THEN 'Wednesday'
        WHEN 4 THEN 'Thursday'
        WHEN 5 THEN 'Friday'
        WHEN 6 THEN 'Saturday'
        WHEN 7 THEN 'Sunday'
        END AS day_name,
    a.start_time,
    a.end_time,
    a.is_active
FROM availabilities a
         JOIN doctors d ON a.doctor_id = d.id
         JOIN users u ON d.user_id = u.id
         JOIN specialties s ON d.specialty_id = s.id
ORDER BY a.doctor_id, a.day_of_week, a.start_time;