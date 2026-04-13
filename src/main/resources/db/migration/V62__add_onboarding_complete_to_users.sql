-- V62: Add onboarding_complete flag to users table
-- Supports the post-registration onboarding flow where new users set their
-- country, currencies, language, date format, number format, and currency
-- display preferences before accessing the main application.
-- Existing users are marked as already onboarded (onboarding_complete = 1).

ALTER TABLE users ADD COLUMN onboarding_complete INTEGER NOT NULL DEFAULT 0;

-- Mark all pre-existing users as having completed onboarding so they are not
-- unexpectedly redirected to the onboarding screen on their next login.
UPDATE users SET onboarding_complete = 1;
