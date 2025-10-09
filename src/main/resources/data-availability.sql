-- เพิ่มตารางเวลาให้แพทย์ทุกคน
DELETE FROM availabilities WHERE doctor_id IN (12,14,15,16,17,18,19,20,21,22,23);

-- Doctor 12: อรรณพ แสงศิลา - Internal Medicine (จันทร์-ศุกร์ 9:00-17:00)
INSERT INTO availabilities (doctor_id, day_of_week, start_time, end_time, is_active, created_at) VALUES
(12, 1, '09:00:00', '17:00:00', true, NOW()),
(12, 2, '09:00:00', '17:00:00', true, NOW()),
(12, 3, '09:00:00', '17:00:00', true, NOW()),
(12, 4, '09:00:00', '17:00:00', true, NOW()),
(12, 5, '09:00:00', '17:00:00', true, NOW());

-- Doctor 14: นพ. พงศกร - Pediatrics (จันทร์-ศุกร์ 8:00-16:00)
INSERT INTO availabilities (doctor_id, day_of_week, start_time, end_time, is_active, created_at) VALUES
(14, 1, '08:00:00', '16:00:00', true, NOW()),
(14, 2, '08:00:00', '16:00:00', true, NOW()),
(14, 3, '08:00:00', '16:00:00', true, NOW()),
(14, 4, '08:00:00', '16:00:00', true, NOW()),
(14, 5, '08:00:00', '16:00:00', true, NOW());

-- Doctor 15: นพ. กิตติศักดิ์ - Cardiology (จันทร์-เสาร์ 9:00-18:00)
INSERT INTO availabilities (doctor_id, day_of_week, start_time, end_time, is_active, created_at) VALUES
(15, 1, '09:00:00', '18:00:00', true, NOW()),
(15, 2, '09:00:00', '18:00:00', true, NOW()),
(15, 3, '09:00:00', '18:00:00', true, NOW()),
(15, 4, '09:00:00', '18:00:00', true, NOW()),
(15, 5, '09:00:00', '18:00:00', true, NOW()),
(15, 6, '09:00:00', '18:00:00', true, NOW());

-- Doctor 16: นพ. ธนพล - Emergency Medicine (ทุกวัน 24 ชม.)
INSERT INTO availabilities (doctor_id, day_of_week, start_time, end_time, is_active, created_at) VALUES
(16, 1, '00:00:00', '23:59:00', true, NOW()),
(16, 2, '00:00:00', '23:59:00', true, NOW()),
(16, 3, '00:00:00', '23:59:00', true, NOW()),
(16, 4, '00:00:00', '23:59:00', true, NOW()),
(16, 5, '00:00:00', '23:59:00', true, NOW()),
(16, 6, '00:00:00', '23:59:00', true, NOW()),
(16, 7, '00:00:00', '23:59:00', true, NOW());

-- Doctor 17: นพ. ศุภกฤต - Internal Medicine (อังคาร-เสาร์ 10:00-18:00)
INSERT INTO availabilities (doctor_id, day_of_week, start_time, end_time, is_active, created_at) VALUES
(17, 2, '10:00:00', '18:00:00', true, NOW()),
(17, 3, '10:00:00', '18:00:00', true, NOW()),
(17, 4, '10:00:00', '18:00:00', true, NOW()),
(17, 5, '10:00:00', '18:00:00', true, NOW()),
(17, 6, '10:00:00', '18:00:00', true, NOW());

-- Doctor 18: นพ. ชยพล - Pediatrics (จันทร์-ศุกร์ เช้า+บ่าย)
INSERT INTO availabilities (doctor_id, day_of_week, start_time, end_time, is_active, created_at) VALUES
(18, 1, '08:00:00', '12:00:00', true, NOW()),(18, 1, '13:00:00', '17:00:00', true, NOW()),
(18, 2, '08:00:00', '12:00:00', true, NOW()),(18, 2, '13:00:00', '17:00:00', true, NOW()),
(18, 3, '08:00:00', '12:00:00', true, NOW()),(18, 3, '13:00:00', '17:00:00', true, NOW()),
(18, 4, '08:00:00', '12:00:00', true, NOW()),(18, 4, '13:00:00', '17:00:00', true, NOW()),
(18, 5, '08:00:00', '12:00:00', true, NOW()),(18, 5, '13:00:00', '17:00:00', true, NOW());

-- Doctor 19: นพ. วีรภัทร - Surgery (จันทร์-ศุกร์ 7:00-15:00)
INSERT INTO availabilities (doctor_id, day_of_week, start_time, end_time, is_active, created_at) VALUES
(19, 1, '07:00:00', '15:00:00', true, NOW()),
(19, 2, '07:00:00', '15:00:00', true, NOW()),
(19, 3, '07:00:00', '15:00:00', true, NOW()),
(19, 4, '07:00:00', '15:00:00', true, NOW()),
(19, 5, '07:00:00', '15:00:00', true, NOW());

-- Doctor 20: นพ. ปริญญา - Cardiology (จ พ ศ 9:00-17:00)
INSERT INTO availabilities (doctor_id, day_of_week, start_time, end_time, is_active, created_at) VALUES
(20, 1, '09:00:00', '17:00:00', true, NOW()),
(20, 3, '09:00:00', '17:00:00', true, NOW()),
(20, 5, '09:00:00', '17:00:00', true, NOW());

-- Doctor 21: นพ. ภาณุวัฒน์ - Emergency Medicine (ทุกวัน 24 ชม.)
INSERT INTO availabilities (doctor_id, day_of_week, start_time, end_time, is_active, created_at) VALUES
(21, 1, '00:00:00', '23:59:00', true, NOW()),
(21, 2, '00:00:00', '23:59:00', true, NOW()),
(21, 3, '00:00:00', '23:59:00', true, NOW()),
(21, 4, '00:00:00', '23:59:00', true, NOW()),
(21, 5, '00:00:00', '23:59:00', true, NOW()),
(21, 6, '00:00:00', '23:59:00', true, NOW()),
(21, 7, '00:00:00', '23:59:00', true, NOW());

-- Doctor 22: นพ. สุรเดช - Internal Medicine เวรบ่าย (จ-ศ 13:00-21:00)
INSERT INTO availabilities (doctor_id, day_of_week, start_time, end_time, is_active, created_at) VALUES
(22, 1, '13:00:00', '21:00:00', true, NOW()),
(22, 2, '13:00:00', '21:00:00', true, NOW()),
(22, 3, '13:00:00', '21:00:00', true, NOW()),
(22, 4, '13:00:00', '21:00:00', true, NOW()),
(22, 5, '13:00:00', '21:00:00', true, NOW());

-- Doctor 23: นพ. อภิสิทธิ์ - Pediatrics (อ-ส 9:00-17:00)
INSERT INTO availabilities (doctor_id, day_of_week, start_time, end_time, is_active, created_at) VALUES
(23, 2, '09:00:00', '17:00:00', true, NOW()),
(23, 3, '09:00:00', '17:00:00', true, NOW()),
(23, 4, '09:00:00', '17:00:00', true, NOW()),
(23, 5, '09:00:00', '17:00:00', true, NOW()),
(23, 6, '09:00:00', '17:00:00', true, NOW());
