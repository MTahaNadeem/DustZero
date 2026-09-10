package com.dustzero.app.models

/**
 * Centralized application constants for DustZero.
 *
 * Edit these values to change the device ID, app version, or other
 * configuration without hunting through multiple files.
 */
object AppConstants {

    // ─── Device ──────────────────────────────────────────────────────────────

    /** The device_id used in Supabase and all cloud communication. */
    const val DEVICE_ID = "dustzero-001"

    /** Human-readable name shown in Settings > Device. */
    const val DEVICE_NAME = "DustZero Controller"

    // ─── App ─────────────────────────────────────────────────────────────────

    const val APP_NAME = "DustZero"
    const val APP_SUBTITLE = "Smart Solar Panel Cleaning System"
    const val APP_VERSION = "1.0.0"

    // ─── Supabase Tables ─────────────────────────────────────────────────────

    const val TABLE_DEVICES = "devices"
    const val TABLE_COMMANDS = "commands"
    const val TABLE_ALERTS = "alerts"

    // ─── Commands ─────────────────────────────────────────────────────────────

    const val CMD_START_CLEANING = "START_CLEANING"
    const val CMD_STOP_CLEANING = "STOP_CLEANING"
    const val CMD_HOME_MOTOR = "HOME_MOTOR"

    // ─── Local Database ───────────────────────────────────────────────────────

    const val LOCAL_DB_NAME = "dustzero_database"
}
