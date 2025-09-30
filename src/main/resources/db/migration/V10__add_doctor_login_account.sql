-- V10__add_doctor_login_account.sql
-- สร้าง account หมอที่ login ได้ รหัส 13

-- เพิ่ม user ใหม่สำหรับหมอ
INSERT INTO users (
    email,
    password,
    first_name,
    last_name,
    role,
    phone,
    created_at,
    updated_at
) VALUES (
    'doctor.arnob@doctora.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password
    'อรรณพ',
    'แสงศิลา',
    'DOCTOR',
    '098-765-4321',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- เพิ่มข้อมูลหมอในตาราง doctors สำหรับ user ที่เพิ่งสร้าง
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
    (SELECT id FROM users WHERE email = 'doctor.arnob@doctora.com'),
    1,
    'DOC-13-2024',
    'แพทย์ผู้เชี่ยวชาญด้านการรักษาและการดูแลผู้ป่วย มีประสบการณ์ในการให้บริการทางการแพทย์มาอย่างยาวนาน',
    5,
    1500.00,
    'A102',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);