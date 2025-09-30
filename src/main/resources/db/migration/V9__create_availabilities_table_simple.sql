-- V9__create_patient_booking_info_table.sql
-- Create patient_booking_info table for storing detailed patient information at booking time

CREATE TABLE patient_booking_info (
    id BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,

    -- Patient details at time of booking
    patient_prefix VARCHAR(20),
    patient_first_name VARCHAR(100),
    patient_last_name VARCHAR(100),
    patient_gender VARCHAR(20),
    patient_date_of_birth DATE,
    patient_nationality VARCHAR(50),
    patient_citizen_id VARCHAR(13),
    patient_phone VARCHAR(20),
    patient_email VARCHAR(255),

    -- Additional booking information
    symptoms TEXT,
    booking_type VARCHAR(20), -- 'auto' or 'manual'
    queue_number VARCHAR(10),

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Ensure one patient_booking_info per appointment
    CONSTRAINT unique_appointment_booking UNIQUE(appointment_id)
);

-- Indexes for performance
CREATE INDEX idx_patient_booking_info_appointment_id ON patient_booking_info(appointment_id);
CREATE INDEX idx_patient_booking_info_email ON patient_booking_info(patient_email);
CREATE INDEX idx_patient_booking_info_citizen_id ON patient_booking_info(patient_citizen_id);
CREATE INDEX idx_patient_booking_info_queue_number ON patient_booking_info(queue_number);
CREATE INDEX idx_patient_booking_info_created_at ON patient_booking_info(created_at);

-- View for easy querying of complete booking information
CREATE VIEW booking_details AS
SELECT
    pbi.id as booking_info_id,
    pbi.queue_number,
    pbi.booking_type,
    pbi.symptoms,
    -- Patient details
    CONCAT(
        COALESCE(pbi.patient_prefix, ''), ' ',
        COALESCE(pbi.patient_first_name, ''), ' ',
        COALESCE(pbi.patient_last_name, '')
    ) AS patient_full_name,
    pbi.patient_gender,
    pbi.patient_date_of_birth,
    pbi.patient_nationality,
    pbi.patient_citizen_id,
    pbi.patient_phone,
    pbi.patient_email,
    -- Appointment details
    a.id as appointment_id,
    a.appointment_datetime,
    a.duration_minutes,
    a.status as appointment_status,
    a.notes,
    -- Doctor details
    d.id as doctor_id,
    doc_user.first_name || ' ' || doc_user.last_name AS doctor_name,
    d.room_number,
    s.name AS specialty_name,
    -- Timestamps
    pbi.created_at as booking_created_at
FROM patient_booking_info pbi
JOIN appointments a ON pbi.appointment_id = a.id
JOIN doctors d ON a.doctor_id = d.id
JOIN users doc_user ON d.user_id = doc_user.id
JOIN specialties s ON d.specialty_id = s.id
ORDER BY pbi.created_at DESC;