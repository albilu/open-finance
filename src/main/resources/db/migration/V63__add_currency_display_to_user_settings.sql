-- V63: Add amount_display_mode preference to user_settings table
-- Controls how monetary amounts are rendered in the UI.
-- Options: 'base' (default) = show base currency, 'native' = show native currency,
--          'both' = show both currencies inline.

ALTER TABLE user_settings ADD COLUMN amount_display_mode VARCHAR(10) NOT NULL DEFAULT 'base';
