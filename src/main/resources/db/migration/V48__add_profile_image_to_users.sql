-- Add profile_image column to users table for storing profile photo as Base64 data URL
ALTER TABLE users ADD COLUMN profile_image TEXT;
