-- Fix the device_history snapshot cron job to properly ignore offline devices
-- and clean up previously duplicated fake data.

-- 1. Redefine the cron job with the correct interval guard
-- We check that updated_at is within the last 5 minutes (since the cron runs every 5 mins).
SELECT cron.schedule(
  'snapshot_device_history_every_5m',
  '*/5 * * * *',
  $$
    INSERT INTO public.device_history (
        device_id, solar_power, solar_voltage, solar_current, temperature,
        ldr1, ldr2, sunlight_level, rain_detected, cleaning_state
    )
    SELECT
        device_id, solar_power, solar_voltage, solar_current, temperature,
        ldr1, ldr2, sunlight_level, rain_detected, cleaning_state
    FROM public.devices
    WHERE connected = true AND updated_at >= now() - interval '6 minutes';
  $$
);

-- 2. Delete the stale/frozen data that was inserted while the device was offline.
DELETE FROM public.device_history
WHERE device_id = 'dustzero-001' 
  AND recorded_at > '2026-09-13T16:40:00+05:00'::timestamptz;
