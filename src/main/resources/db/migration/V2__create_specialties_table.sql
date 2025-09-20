-- สร้างตารางแผนก/สาขาเชี่ยวชาญสำหรับโรงพยาบาลศรีขอนแก่น

CREATE TABLE specialties
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index สำหรับการค้นหา
CREATE INDEX idx_specialties_name ON specialties (name);

-- Mock ข้อมูลแผนกก่อนเฉยๆ ค่อยมาคุยเพิ่มทีหลัง
INSERT INTO specialties (name, description)
VALUES ('Internal Medicine', 'อายุรกรรม'),
       ('Surgery', 'ศัลยกรรม'),
       ('Pediatrics', 'กุมารเวชกรรม'),
       ('Cardiology', 'หัวใจและหลอดเลือด'),
       ('Emergency Medicine', 'แพทย์ฉุกเฉิน');