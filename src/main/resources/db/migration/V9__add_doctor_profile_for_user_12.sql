-- V9__add_doctor_profile_for_user_12.sql
-- เพิ่มข้อมูลหมอสำหรับ user id = 12 (อรรณพ แสงศิลา)

-- ก่อนอื่น ตรวจสอบว่า user id = 12 มีอยู่จริง และเปลี่ยน role เป็น DOCTOR
UPDATE users
SET role = 'DOCTOR'
WHERE id = 12;

-- เพิ่มข้อมูลหมอในตาราง doctors
-- ใช้ specialty_id = 1 (สมมติว่าเป็นแผนกแรก) ถ้าไม่มีจะใช้ DEFAULT
INSERT INTO doctors (
    user_id,
    specialty_id,
    license_number,
    bio,
    experience_years,
    consultation_fee,
    room_number,
    is_active,
    created_at,
    updated_at
) VALUES (
    12,  -- user_id
    1,   -- specialty_id (แผนกแรกในระบบ)
    'DOC-12-2024',  -- license_number
    'แพทย์ผู้เชี่ยวชาญด้านการรักษาและการดูแลผู้ป่วย มีประสบการณ์ในการให้บริการทางการแพทย์มาอย่างยาวนาน', -- bio
    5,   -- experience_years
    1500.00,  -- consultation_fee
    'A101',   -- room_number
    true,     -- is_active
    CURRENT_TIMESTAMP,  -- created_at
    CURRENT_TIMESTAMP   -- updated_at
);

-- เพิ่มข้อมูลเพิ่มเติมถ้าต้องการ (ตัวอย่าง)
-- UPDATE users SET first_name = 'อรรณพ', last_name = 'แสงศิลา' WHERE id = 12;