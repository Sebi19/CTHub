-- 1. Add the new columns as nullable to support legacy scraped data
ALTER TABLE competition ADD COLUMN slug VARCHAR(255);
ALTER TABLE competition ADD COLUMN flow_id INTEGER;
ALTER TABLE competition ADD COLUMN challenge_id INTEGER;

-- 2. Optional but recommended: Add indexes if you plan to query/lookup by these IDs during the daily sync
CREATE INDEX idx_competition_slug ON competition(slug);
CREATE INDEX idx_competition_flow_id ON competition(flow_id);
CREATE INDEX idx_competition_challenge_id ON competition(challenge_id);