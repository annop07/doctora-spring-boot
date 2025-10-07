-- V11: เพิ่มตารางเวลาให้แพทย์ที่มีอยู่ในระบบ
-- ใช้ DO block เพื่อจัดการกับ doctor_id แบบ dynamic

DO $$
DECLARE
    v_doctor_id BIGINT;
BEGIN
    -- หา doctor_id จาก email ของหมอที่สร้างใน V10
    SELECT d.id INTO v_doctor_id
    FROM doctors d
    JOIN users u ON d.user_id = u.id
    WHERE u.email = 'doctor.arnob@doctora.com';

    -- ถ้าพบ doctor ให้เพิ่ม availability
    IF v_doctor_id IS NOT NULL THEN
        -- ลบตารางเวลาเก่าถ้ามี (safety check)
        DELETE FROM availabilities WHERE doctor_id = v_doctor_id;

        -- เพิ่มตารางเวลาใหม่: อรรณพ แสงศิลา - Internal Medicine (จันทร์-ศุกร์ 9:00-17:00)
        INSERT INTO availabilities (doctor_id, day_of_week, start_time, end_time, is_active, created_at) VALUES
        (v_doctor_id, 1, '09:00:00', '17:00:00', true, CURRENT_TIMESTAMP),
        (v_doctor_id, 2, '09:00:00', '17:00:00', true, CURRENT_TIMESTAMP),
        (v_doctor_id, 3, '09:00:00', '17:00:00', true, CURRENT_TIMESTAMP),
        (v_doctor_id, 4, '09:00:00', '17:00:00', true, CURRENT_TIMESTAMP),
        (v_doctor_id, 5, '09:00:00', '17:00:00', true, CURRENT_TIMESTAMP);

        RAISE NOTICE 'Added availability schedule for doctor ID: %', v_doctor_id;
    ELSE
        RAISE NOTICE 'Doctor not found. Skipping availability creation.';
    END IF;
END $$;