CREATE TABLE IF NOT EXISTS cleaning_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT REFERENCES devices(device_id) ON DELETE CASCADE,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE,
    power_before NUMERIC,
    power_after NUMERIC,
    power_delta NUMERIC,
    sunlight_level TEXT,
    trigger TEXT
);

ALTER TABLE cleaning_events ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view cleaning events for own devices" ON cleaning_events
    FOR SELECT TO authenticated
    USING (device_id IN (SELECT device_id FROM devices WHERE user_id = auth.uid()));

-- We will create a database function and trigger to populate this automatically.
-- The ESP32 changes cleaning_state from 'IDLE' to something else, and back to 'IDLE'.

CREATE OR REPLACE FUNCTION log_cleaning_event()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.cleaning_state = 'IDLE' AND NEW.cleaning_state != 'IDLE' THEN
        -- Cleaning started
        INSERT INTO cleaning_events (device_id, started_at, power_before, sunlight_level, trigger)
        VALUES (NEW.device_id, now(), OLD.solar_power, NEW.sunlight_level, 'AUTO');
    ELSIF OLD.cleaning_state != 'IDLE' AND NEW.cleaning_state = 'IDLE' THEN
        -- Cleaning ended
        UPDATE cleaning_events
        SET ended_at = now(),
            power_after = NEW.solar_power,
            power_delta = NEW.solar_power - power_before
        WHERE device_id = NEW.device_id AND ended_at IS NULL
        -- Update the most recent unfinished event
        AND id = (SELECT id FROM cleaning_events WHERE device_id = NEW.device_id AND ended_at IS NULL ORDER BY started_at DESC LIMIT 1);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trigger_log_cleaning_event ON devices;
CREATE TRIGGER trigger_log_cleaning_event
AFTER UPDATE OF cleaning_state ON devices
FOR EACH ROW
WHEN (OLD.cleaning_state IS DISTINCT FROM NEW.cleaning_state)
EXECUTE FUNCTION log_cleaning_event();
