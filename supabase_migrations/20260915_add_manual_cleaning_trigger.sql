CREATE OR REPLACE FUNCTION log_cleaning_event()
RETURNS TRIGGER AS $$
DECLARE
    recent_manual_cmd_count INT;
    trigger_type TEXT;
BEGIN
    IF OLD.cleaning_state = 'IDLE' AND NEW.cleaning_state != 'IDLE' THEN
        -- Check if a START_MANUAL_CLEANING command was sent in the last 2 minutes
        SELECT count(*) INTO recent_manual_cmd_count
        FROM commands
        WHERE device_id = NEW.device_id 
          AND command = 'START_MANUAL_CLEANING' 
          AND created_at > now() - interval '2 minutes';
        
        IF recent_manual_cmd_count > 0 THEN
            trigger_type := 'MANUAL';
        ELSE
            trigger_type := 'AUTO';
        END IF;

        -- Cleaning started
        INSERT INTO cleaning_events (device_id, started_at, power_before, sunlight_level, trigger)
        VALUES (NEW.device_id, now(), OLD.solar_power, NEW.sunlight_level, trigger_type);
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
