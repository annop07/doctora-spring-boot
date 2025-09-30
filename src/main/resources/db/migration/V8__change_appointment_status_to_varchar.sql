-- V9__create_availabilities_table_simple.sql
-- Simple availability table without complex constraints

CREATE TABLE IF NOT EXISTS availabilities (
    id BIGSERIAL PRIMARY KEY,
    doctor_id BIGINT NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    day_of_week INTEGER NOT NULL CHECK (day_of_week >= 1 AND day_of_week <= 7),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT check_time_range CHECK (start_time < end_time)
);

CREATE INDEX idx_availabilities_doctor_id ON availabilities(doctor_id);
CREATE INDEX idx_availabilities_day_active ON availabilities(day_of_week, is_active);

-- ============================================
-- DOCTOR 1: Kobe Bryant (ID: 1)
-- Schedule: Full-time, Monday to Friday
-- Morning: 09:00-12:00, Afternoon: 13:00-17:00
-- ============================================

-- Monday
INSERT INTO availabilities (doctor_id, day_of_week, start_time, end_time, is_active) VALUES
(1, 1, '09:00:00', '12:00:00', true),  -- Morning session
(1, 1, '13:00:00', '17:00:00', true);  -- Afternoon session

-- Tuesday
INSERT INTO availabilities (doctor_id, day_of_week, start_time, end_time, is_active) VALUES
(1, 2, '09:00:00', '12:00:00', true),
(1, 2, '13:00:00', '17:00:00', true);

-- Wednesday
INSERT INTO availabilities (doctor_id, day_of_week, start_time, end_time, is_active) VALUES
(1, 3, '09:00:00', '12:00:00', true),
(1, 3, '13:00:00', '17:00:00', true);

-- Thursday
INSERT INTO availabilities (doctor_id, day_of_week, start_time, end_time, is_active) VALUES
(1, 4, '09:00:00', '12:00:00', true),
(1, 4, '13:00:00', '17:00:00', true);

-- Friday
INSERT INTO availabilities (doctor_id, day_of_week, start_time, end_time, is_active) VALUES
(1, 5, '09:00:00', '12:00:00', true),
(1, 5, '13:00:00', '17:00:00', true);

-- ============================================
-- DOCTOR 2: Jaylen Brown (ID: 2)
-- Schedule: Part-time, Monday/Wednesday/Friday + Saturday morning
-- Weekday: 10:00-13:00, 14:00-18:00
-- Saturday: 09:00-13:00 only
-- ============================================

-- Monday
INSERT INTO availabilities (doctor_id, day_of_week, start_time, end_time, is_active) VALUES
(2, 1, '10:00:00', '13:00:00', true),
(2, 1, '14:00:00', '18:00:00', true);

-- Wednesday
INSERT INTO availabilities (doctor_id, day_of_week, start_time, end_time, is_active) VALUES
(2, 3, '10:00:00', '13:00:00', true),
(2, 3, '14:00:00', '18:00:00', true);

-- Friday
INSERT INTO availabilities (doctor_id, day_of_week, start_time, end_time, is_active) VALUES
(2, 5, '10:00:00', '13:00:00', true),
(2, 5, '14:00:00', '18:00:00', true);

-- Saturday (morning only)
INSERT INTO availabilities (doctor_id, day_of_week, start_time, end_time, is_active) VALUES
(2, 6, '09:00:00', '13:00:00', true);