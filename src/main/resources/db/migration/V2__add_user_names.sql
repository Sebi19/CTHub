ALTER TABLE app_user ADD COLUMN first_name VARCHAR(255);
ALTER TABLE app_user ADD COLUMN last_name VARCHAR(255);

-- 2. Provide a default value for existing rows so the NOT NULL constraint doesn't fail
UPDATE app_user SET first_name = 'Admin', last_name = 'Admin' WHERE first_name IS NULL;

-- 3. Now apply the NOT NULL constraint
ALTER TABLE app_user ALTER COLUMN first_name SET NOT NULL;
ALTER TABLE app_user ALTER COLUMN last_name SET NOT NULL;