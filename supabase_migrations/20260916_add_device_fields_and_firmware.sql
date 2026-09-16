-- 1. Add device location fields and device name to `devices` table
ALTER TABLE devices 
ADD COLUMN IF NOT EXISTS latitude NUMERIC,
ADD COLUMN IF NOT EXISTS longitude NUMERIC,
ADD COLUMN IF NOT EXISTS device_name TEXT;

-- 2. Create the firmware_releases table
CREATE TABLE IF NOT EXISTS firmware_releases (
    version TEXT PRIMARY KEY,
    released_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    release_notes TEXT,
    is_latest BOOLEAN DEFAULT false
);

-- 3. Enable RLS and allow public read access
ALTER TABLE firmware_releases ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow public read access to firmware_releases" 
ON firmware_releases 
FOR SELECT 
USING (true);
