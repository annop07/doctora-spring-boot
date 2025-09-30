-- V8__change_appointment_status_to_varchar.sql
-- Change appointment status from ENUM to VARCHAR for better flexibility

-- Step 1: Drop the view that depends on the status column
DROP VIEW IF EXISTS appointment_details;

-- Step 2: Add a temporary column
ALTER TABLE appointments ADD COLUMN status_temp VARCHAR(20);

-- Step 3: Copy data from ENUM to VARCHAR
UPDATE appointments SET status_temp = status::text;

-- Step 4: Drop the old ENUM column
ALTER TABLE appointments DROP COLUMN status;

-- Step 5: Rename the temp column
ALTER TABLE appointments RENAME COLUMN status_temp TO status;

-- Step 6: Set default value
ALTER TABLE appointments ALTER COLUMN status SET DEFAULT 'PENDING';

-- Step 7: Add check constraint (optional, for data validation)
ALTER TABLE appointments ADD CONSTRAINT check_appointment_status
    CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW'));

-- Step 8: Recreate the appointment_details view
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