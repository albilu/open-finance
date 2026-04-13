-- Add payment_method column to transactions table
-- Feature: Add Payment Method field to transaction form

ALTER TABLE transactions ADD COLUMN payment_method VARCHAR(20);

-- Add index for payment_method to improve filtering performance
CREATE INDEX idx_transaction_payment_method ON transactions(payment_method);
