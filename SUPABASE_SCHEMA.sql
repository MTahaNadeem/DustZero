-- Supabase Schema for SolarClean AI

-- 1. Create the devices table
CREATE TABLE devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT UNIQUE NOT NULL,
    connected BOOLEAN DEFAULT false,
    ldr1 INTEGER DEFAULT 0,
    ldr2 INTEGER DEFAULT 0,
    temperature NUMERIC DEFAULT 0.0,
    solar_voltage NUMERIC DEFAULT 0.0,
    solar_current NUMERIC DEFAULT 0.0,
    solar_power NUMERIC DEFAULT 0.0,
    rain_detected BOOLEAN DEFAULT false,
    sun_detected BOOLEAN DEFAULT false,
    sunlight_level TEXT DEFAULT 'WEAK',
    cleaning_state TEXT DEFAULT 'IDLE',
    cleaning_progress INTEGER DEFAULT 0,
    cleaning_steps INTEGER DEFAULT 0,
    fault BOOLEAN DEFAULT false,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- 2. Create the commands table for App -> ESP32 communication
CREATE TABLE commands (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT REFERENCES devices(device_id),
    command TEXT NOT NULL,
    status TEXT DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- 3. Enable Realtime for these tables
ALTER PUBLICATION supabase_realtime ADD TABLE devices;
ALTER PUBLICATION supabase_realtime ADD TABLE commands;

-- 4. Initial mock device
INSERT INTO devices (device_id, connected) VALUES ('solarclean-001', false) ON CONFLICT DO NOTHING;
