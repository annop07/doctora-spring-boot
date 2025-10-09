CREATE TABLE patient_booking_info (
    id BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT NOT NULL,
    patient_prefix VARCHAR(20),
    patient_first_name VARCHAR(100),
    patient_last_name VARCHAR(100),
    patient_gender VARCHAR(20),
    patient_date_of_birth DATE,
    patient_nationality VARCHAR(50),
    patient_citizen_id VARCHAR(13),
    patient_phone VARCHAR(20),
    patient_email VARCHAR(255),
    symptoms TEXT,
    booking_type VARCHAR(20),
    queue_number VARCHAR(10),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_patient_booking_info_appointment
        FOREIGN KEY (appointment_id)
        REFERENCES appointments(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_patient_booking_info_appointment_id ON patient_booking_info(appointment_id);
