-- V10__add_doctor_login_account.sql
-- สร้าง account หมอที่ login ได้

-- เพิ่ม user ใหม่สำหรับหมอ (ถ้ายังไม่มี)
INSERT INTO users (
    email,
    password,
    first_name,
    last_name,
    role,
    phone,
    created_at,
    updated_at
)
SELECT
    'doctor.arnob@doctora.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password
    'อรรณพ',
    'แสงศิลา',
    'DOCTOR',
    '098-765-4321',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'doctor.arnob@doctora.com'
);

-- เพิ่มข้อมูลหมอในตาราง doctors (ถ้ายังไม่มี)
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
)
SELECT
    u.id,
    1, -- Internal Medicine
    'DOC-ARN-2024',
    'แพทย์ผู้เชี่ยวชาญด้านการรักษาและการดูแลผู้ป่วย มีประสบการณ์ในการให้บริการทางการแพทย์มาอย่างยาวนาน',
    5,
    1500.00,
    'A102',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'doctor.arnob@doctora.com'
AND NOT EXISTS (
    SELECT 1 FROM doctors d WHERE d.user_id = u.id
);