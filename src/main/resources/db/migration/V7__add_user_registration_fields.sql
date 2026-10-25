ALTER TABLE app_user ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE app_user ADD COLUMN email_verified_at TIMESTAMPTZ;
ALTER TABLE app_user ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE app_user ADD COLUMN preferred_locale VARCHAR(8);

-- Grandfather existing accounts (currently only the seeded admin) so they don't lock themselves out
-- now that User.isEnabled() actually reads this column instead of being hardcoded to true.
UPDATE app_user SET enabled = TRUE, email_verified_at = now();
