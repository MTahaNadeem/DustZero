-- Add user_id to devices table to establish ownership
ALTER TABLE devices
ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL;

-- Enable RLS on devices
ALTER TABLE devices ENABLE ROW LEVEL SECURITY;

-- Allow authenticated users to view and update their own devices
CREATE POLICY "Users can view own devices" ON devices
    FOR SELECT TO authenticated
    USING (auth.uid() = user_id);

CREATE POLICY "Users can update own devices" ON devices
    FOR UPDATE TO authenticated
    USING (auth.uid() = user_id);

-- Allow anonymous role (ESP32) to update its own row by device_id
-- We cannot use auth.uid() since it's anonymous, so we allow update if the row exists.
-- A stricter policy would require a secret, but for now we limit it to the anon key.
CREATE POLICY "ESP32 can update its own row" ON devices
    FOR UPDATE TO anon
    USING (true)
    WITH CHECK (true);

CREATE POLICY "ESP32 can read its own row" ON devices
    FOR SELECT TO anon
    USING (true);

-- Enable RLS on commands
ALTER TABLE commands ENABLE ROW LEVEL SECURITY;

-- Allow authenticated users to insert/read commands for their devices
CREATE POLICY "Users can insert commands for own devices" ON commands
    FOR INSERT TO authenticated
    WITH CHECK (
        device_id IN (SELECT device_id FROM devices WHERE user_id = auth.uid())
    );

CREATE POLICY "Users can view commands for own devices" ON commands
    FOR SELECT TO authenticated
    USING (
        device_id IN (SELECT device_id FROM devices WHERE user_id = auth.uid())
    );

-- Allow anonymous role (ESP32) to read and update commands
CREATE POLICY "ESP32 can read commands" ON commands
    FOR SELECT TO anon
    USING (true);

CREATE POLICY "ESP32 can update commands" ON commands
    FOR UPDATE TO anon
    USING (true)
    WITH CHECK (true);
