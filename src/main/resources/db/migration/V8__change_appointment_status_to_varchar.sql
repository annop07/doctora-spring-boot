-- V8__change_appointment_status_to_varchar.sql
-- Change appointment status from enum to varchar for easier Hibernate integration

-- First, drop the dependent view
DROP VIEW IF EXISTS appointment_details;

-- Add a new varchar column
ALTER TABLE appointments ADD COLUMN status_varchar VARCHAR(20);

-- Copy existing enum values to varchar column
UPDATE appointments SET status_varchar = status::text;

-- Set default value for new records
ALTER TABLE appointments ALTER COLUMN status_varchar SET DEFAULT 'PENDING';

-- Make the new column NOT NULL
ALTER TABLE appointments ALTER COLUMN status_varchar SET NOT NULL;

-- Drop the old enum column
ALTER TABLE appointments DROP COLUMN status;

-- Rename the new column to status
ALTER TABLE appointments RENAME COLUMN status_varchar TO status;

-- Add check constraint to ensure only valid values
ALTER TABLE appointments ADD CONSTRAINT appointment_status_check
    CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW'));

-- Update the appointment_details view to use the new column
DROP VIEW IF EXISTS appointment_details;

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