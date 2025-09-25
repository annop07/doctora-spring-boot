-- V7: Insert Complete Doctor Test Data and Availabilities
-- สร้างข้อมูลหมอและตารางเวลาทำงานสำหรับทดสอบ

-- First, insert sample users for doctors
INSERT INTO users (first_name, last_name, email, phone, password, role, created_at, updated_at) VALUES
('สมชาย', 'ใจดี', 'somchai.doctor@hospital.com', '0812345678', '$2a$10$example', 'DOCTOR', NOW(), NOW()),
('สิริพร', 'สุขใส', 'siriporn.doctor@hospital.com', '0823456789', '$2a$10$example', 'DOCTOR', NOW(), NOW()),
('ประเสริฐ', 'มั่นคง', 'prasert.doctor@hospital.com', '0834567890', '$2a$10$example', 'DOCTOR', NOW(), NOW()),
('วนิดา', 'เก่งกาจ', 'wanida.doctor@hospital.com', '0845678901', '$2a$10$example', 'DOCTOR', NOW(), NOW()),
('ศักดิ์ชัย', 'ช่วยเหลือ', 'sakchai.doctor@hospital.com', '0856789012', '$2a$10$example', 'DOCTOR', NOW(), NOW());

-- Then, insert doctors using the user IDs
INSERT INTO doctors (user_id, specialty_id, license_number, bio, experience_years, consultation_fee, room_number, is_active, created_at, updated_at) VALUES
((SELECT id FROM users WHERE email = 'somchai.doctor@hospital.com'), 4, 'MD001', 'แพทย์ผู้เชี่ยวชาญด้านหัวใจและหลอดเลือด', 15, 800.00, 'A101', true, NOW(), NOW()),
((SELECT id FROM users WHERE email = 'siriporn.doctor@hospital.com'), 1, 'MD002', 'แพทย์ผู้เชี่ยวชาญด้านโรคผิวหนัง', 12, 600.00, 'B205', true, NOW(), NOW()),
((SELECT id FROM users WHERE email = 'prasert.doctor@hospital.com'), 2, 'MD003', 'แพทย์ผู้เชี่ยวชาญด้านศัลยกรรมกระดูก', 18, 900.00, 'C301', true, NOW(), NOW()),
((SELECT id FROM users WHERE email = 'wanida.doctor@hospital.com'), 3, 'MD004', 'แพทย์ผู้เชี่ยวชาญด้านเด็ก', 10, 550.00, 'D102', true, NOW(), NOW()),
((SELECT id FROM users WHERE email = 'sakchai.doctor@hospital.com'), 1, 'MD005', 'แพทย์ทั่วไป ฉุกเฉิน', 8, 500.00, 'E401', true, NOW(), NOW());

-- Now insert availability schedules for each doctor

-- Dr. Somchai (Cardiology) - จันทร์-ศุกร์ 8:00-16:00
INSERT INTO availabilities (doctor_id, day_of_week, start_time, end_time, is_active, created_at, updated_at) VALUES
((SELECT id FROM doctors WHERE license_number = 'MD001'), 1, '08:00:00', '16:00:00', true, NOW(), NOW()), -- Monday
((SELECT id FROM doctors WHERE license_number = 'MD001'), 2, '08:00:00', '16:00:00', true, NOW(), NOW()), -- Tuesday
((SELECT id FROM doctors WHERE license_number = 'MD001'), 3, '08:00:00', '16:00:00', true, NOW(), NOW()), -- Wednesday
((SELECT id FROM doctors WHERE license_number = 'MD001'), 4, '08:00:00', '16:00:00', true, NOW(), NOW()), -- Thursday
((SELECT id FROM doctors WHERE license_number = 'MD001'), 5, '08:00:00', '16:00:00', true, NOW(), NOW()); -- Friday

-- Dr. Siriporn (Internal Medicine) - จันทร์-ศุกร์ 9:00-17:00
INSERT INTO availabilities (doctor_id, day_of_week, start_time, end_time, is_active, created_at, updated_at) VALUES
((SELECT id FROM doctors WHERE license_number = 'MD002'), 1, '09:00:00', '17:00:00', true, NOW(), NOW()), -- Monday
((SELECT id FROM doctors WHERE license_number = 'MD002'), 2, '09:00:00', '17:00:00', true, NOW(), NOW()), -- Tuesday
((SELECT id FROM doctors WHERE license_number = 'MD002'), 3, '09:00:00', '17:00:00', true, NOW(), NOW()), -- Wednesday
((SELECT id FROM doctors WHERE license_number = 'MD002'), 4, '09:00:00', '17:00:00', true, NOW(), NOW()), -- Thursday
((SELECT id FROM doctors WHERE license_number = 'MD002'), 5, '09:00:00', '17:00:00', true, NOW(), NOW()); -- Friday

-- Dr. Prasert (Surgery) - จันทร์-เสาร์ 7:30-15:30
INSERT INTO availabilities (doctor_id, day_of_week, start_time, end_time, is_active, created_at, updated_at) VALUES
((SELECT id FROM doctors WHERE license_number = 'MD003'), 1, '07:30:00', '15:30:00', true, NOW(), NOW()), -- Monday
((SELECT id FROM doctors WHERE license_number = 'MD003'), 2, '07:30:00', '15:30:00', true, NOW(), NOW()), -- Tuesday
((SELECT id FROM doctors WHERE license_number = 'MD003'), 3, '07:30:00', '15:30:00', true, NOW(), NOW()), -- Wednesday
((SELECT id FROM doctors WHERE license_number = 'MD003'), 4, '07:30:00', '15:30:00', true, NOW(), NOW()), -- Thursday
((SELECT id FROM doctors WHERE license_number = 'MD003'), 5, '07:30:00', '15:30:00', true, NOW(), NOW()), -- Friday
((SELECT id FROM doctors WHERE license_number = 'MD003'), 6, '08:00:00', '12:00:00', true, NOW(), NOW()); -- Saturday

-- Dr. Wanida (Pediatrics) - จันทร์-ศุกร์ 10:00-18:00 + เสาร์ 9:00-13:00
INSERT INTO availabilities (doctor_id, day_of_week, start_time, end_time, is_active, created_at, updated_at) VALUES
((SELECT id FROM doctors WHERE license_number = 'MD004'), 1, '10:00:00', '18:00:00', true, NOW(), NOW()), -- Monday
((SELECT id FROM doctors WHERE license_number = 'MD004'), 2, '10:00:00', '18:00:00', true, NOW(), NOW()), -- Tuesday
((SELECT id FROM doctors WHERE license_number = 'MD004'), 3, '10:00:00', '18:00:00', true, NOW(), NOW()), -- Wednesday
((SELECT id FROM doctors WHERE license_number = 'MD004'), 4, '10:00:00', '18:00:00', true, NOW(), NOW()), -- Thursday
((SELECT id FROM doctors WHERE license_number = 'MD004'), 5, '10:00:00', '18:00:00', true, NOW(), NOW()), -- Friday
((SELECT id FROM doctors WHERE license_number = 'MD004'), 6, '09:00:00', '13:00:00', true, NOW(), NOW()); -- Saturday

-- Dr. Sakchai (Internal Medicine/Emergency) - ทุกวัน 8:00-20:00
INSERT INTO availabilities (doctor_id, day_of_week, start_time, end_time, is_active, created_at, updated_at) VALUES
((SELECT id FROM doctors WHERE license_number = 'MD005'), 1, '08:00:00', '20:00:00', true, NOW(), NOW()), -- Monday
((SELECT id FROM doctors WHERE license_number = 'MD005'), 2, '08:00:00', '20:00:00', true, NOW(), NOW()), -- Tuesday
((SELECT id FROM doctors WHERE license_number = 'MD005'), 3, '08:00:00', '20:00:00', true, NOW(), NOW()), -- Wednesday
((SELECT id FROM doctors WHERE license_number = 'MD005'), 4, '08:00:00', '20:00:00', true, NOW(), NOW()), -- Thursday
((SELECT id FROM doctors WHERE license_number = 'MD005'), 5, '08:00:00', '20:00:00', true, NOW(), NOW()), -- Friday
((SELECT id FROM doctors WHERE license_number = 'MD005'), 6, '08:00:00', '20:00:00', true, NOW(), NOW()), -- Saturday
((SELECT id FROM doctors WHERE license_number = 'MD005'), 7, '08:00:00', '20:00:00', true, NOW(), NOW()); -- Sunday