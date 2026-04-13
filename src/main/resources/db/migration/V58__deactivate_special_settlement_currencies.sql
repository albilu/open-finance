-- V58: Deactivate special interbank settlement currencies
-- CHE (WIR Euro) and CHW (WIR Franc) are Swiss interbank clearing currencies
-- CLF (Chilean Unidad de Fomento) and COU (Unidad de Valor Real, Colombia) are
-- special non-physical settlement units — they should not appear as selectable
-- currencies for regular users.
UPDATE currencies SET is_active = 0 WHERE code IN ('CHE', 'CHW', 'CLF', 'COU');
