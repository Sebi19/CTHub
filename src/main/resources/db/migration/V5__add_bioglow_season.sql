-- 1. Ensure all current seasons are marked as inactive
UPDATE season SET active = false WHERE active = true;

-- 2. Insert the new season and set it as active
INSERT INTO season (id, active, name, overview_hash, start_year, max_points)
VALUES ('2026-27', true, 'Bioglow', NULL, 2026, 0);