-- Adds a free-text note to time log entries, so a technician can record what was done during
-- a clocked session (F6.2: "Time entries record minutes and an optional note").

ALTER TABLE time_logs ADD COLUMN note TEXT NULL AFTER minutes_logged;
