CREATE TABLE IF NOT EXISTS device_baselines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT REFERENCES devices(device_id) ON DELETE CASCADE,
    sunlight_level TEXT NOT NULL,
    baseline_power NUMERIC NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    notes TEXT
);

-- Enable RLS
ALTER TABLE device_baselines ENABLE ROW LEVEL SECURITY;

-- Allow users to manage their device baselines
CREATE POLICY "Users can view baselines for own devices" ON device_baselines
    FOR SELECT TO authenticated
    USING (device_id IN (SELECT device_id FROM devices WHERE user_id = auth.uid()));

CREATE POLICY "Users can insert baselines for own devices" ON device_baselines
    FOR INSERT TO authenticated
    WITH CHECK (device_id IN (SELECT device_id FROM devices WHERE user_id = auth.uid()));

CREATE POLICY "Users can update baselines for own devices" ON device_baselines
    FOR UPDATE TO authenticated
    USING (device_id IN (SELECT device_id FROM devices WHERE user_id = auth.uid()));

CREATE POLICY "Users can delete baselines for own devices" ON device_baselines
    FOR DELETE TO authenticated
    USING (device_id IN (SELECT device_id FROM devices WHERE user_id = auth.uid()));
