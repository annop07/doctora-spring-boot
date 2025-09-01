-- สร้างตาราง doctors สำหรับโรงพยาบาลศรีขอนแก่น

CREATE TABLE doctors
(
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT              NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    specialty_id     BIGINT              NOT NULL REFERENCES specialties (id) ON DELETE RESTRICT,
    license_number   VARCHAR(100) UNIQUE NOT NULL,
    bio              TEXT,
    experience_years INTEGER                  DEFAULT 0,
    consultation_fee DECIMAL(10, 2)           DEFAULT 500.00, -- ค่าตรวจเริ่มต้น 500 บาท
    room_number      VARCHAR(20),                             -- เลขห้องตรวจ
    is_active        BOOLEAN                  DEFAULT true,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP

    -- Note: Business logic จะต้องตรวจสอบว่า user มี role = 'DOCTOR' ใน application layer
    -- เนื่องจาก PostgreSQL ไม่อนุญาตให้ใช้ subquery ใน CHECK constraint
);

-- Index สำหรับ foreign keys และการค้นหา
CREATE INDEX idx_doctors_user_id ON doctors (user_id);
CREATE INDEX idx_doctors_specialty_id ON doctors (specialty_id);
CREATE INDEX idx_doctors_license ON doctors (license_number);
CREATE INDEX idx_doctors_active ON doctors (is_active);

-- Unique constraint: หนึ่ง user มีได้แค่หนึ่ง doctor profile
CREATE UNIQUE INDEX idx_doctors_user_unique ON doctors (user_id);