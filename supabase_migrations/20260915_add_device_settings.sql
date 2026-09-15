CREATE TABLE IF NOT EXISTS device_settings (
    device_id TEXT PRIMARY KEY REFERENCES devices(device_id) ON DELETE CASCADE,
    automatic_cleaning BOOLEAN DEFAULT true,
    power_threshold NUMERIC DEFAULT 0.05,
    sunlight_threshold INTEGER DEFAULT 200,
    cleaning_distance_steps INTEGER DEFAULT 500,
    cleaning_cooldown_minutes INTEGER DEFAULT 30,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

ALTER TABLE device_settings ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view settings for own devices" ON device_settings
    FOR SELECT TO authenticated
    USING (device_id IN (SELECT device_id FROM devices WHERE user_id = auth.uid()));

CREATE POLICY "Users can update settings for own devices" ON device_settings
    FOR UPDATE TO authenticated
    USING (device_id IN (SELECT device_id FROM devices WHERE user_id = auth.uid()));

CREATE POLICY "Users can insert settings for own devices" ON device_settings
    FOR INSERT TO authenticated
    WITH CHECK (device_id IN (SELECT device_id FROM devices WHERE user_id = auth.uid()));

-- Automatically create device_settings row when a device is created (optional, but helpful)
CREATE OR REPLACE FUNCTION create_default_device_settings()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO device_settings (device_id) VALUES (NEW.device_id)
    ON CONFLICT (device_id) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trigger_create_default_device_settings ON devices;
CREATE TRIGGER trigger_create_default_device_settings
AFTER INSERT ON devices
FOR EACH ROW
EXECUTE FUNCTION create_default_device_settings();
